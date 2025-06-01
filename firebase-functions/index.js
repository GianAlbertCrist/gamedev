const functions = require("firebase-functions")
const admin = require("firebase-admin")
admin.initializeApp()

// Cloud function that runs daily to check for due transactions and send notifications
exports.checkRecurringTransactions = functions.pubsub
  .schedule("0 8 * * *")
  .timeZone("Asia/Manila")
  .onRun(async (context) => {
    const db = admin.firestore()
    const now = new Date()

    // Create today's date in Manila timezone for accurate comparison
    const today = new Date(now.toLocaleString("en-US", { timeZone: "Asia/Manila" }))
    today.setHours(0, 0, 0, 0)

    const endOfDay = new Date(today)
    endOfDay.setHours(23, 59, 59, 999)

    console.log(`=== NOTIFICATION CHECK STARTED ===`)
    console.log(`Current UTC time: ${now.toISOString()}`)
    console.log(`Manila time: ${now.toLocaleString("en-US", { timeZone: "Asia/Manila" })}`)
    console.log(`Checking for transactions due between ${today.toISOString()} and ${endOfDay.toISOString()}`)

    try {
      const usersSnapshot = await db.collection("users").get()
      console.log(`Found ${usersSnapshot.size} users to check`)

      let totalNotificationsSent = 0
      let totalTransactionsProcessed = 0

      for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id
        console.log(`\n--- Checking user: ${userId} ---`)

        // Get user's FCM token first to avoid unnecessary queries
        const tokenDoc = await db.collection("users").doc(userId).collection("tokens").doc("fcm").get()

        if (!tokenDoc.exists) {
          console.log(`❌ No FCM token document found for user ${userId}`)
          continue
        }

        const tokenData = tokenDoc.data()
        const fcmToken = tokenData?.fcmToken

        if (!fcmToken) {
          console.log(`❌ FCM token is empty for user ${userId}`)
          continue
        }

        console.log(`✅ FCM token found for user ${userId}: ${fcmToken.substring(0, 20)}...`)
        console.log(`Token last updated: ${tokenData.lastUpdated?.toDate?.()?.toISOString() || "Unknown"}`)

        const transactionsSnapshot = await db
          .collection("users")
          .doc(userId)
          .collection("transactions")
          .where("nextDueDate", ">=", today)
          .where("nextDueDate", "<=", endOfDay)
          .where("recurring", "!=", "None")
          .get()

        console.log(`Found ${transactionsSnapshot.size} due transactions for user ${userId}`)

        // Process each due transaction
        for (const transactionDoc of transactionsSnapshot.docs) {
          const transaction = transactionDoc.data()
          totalTransactionsProcessed++

          console.log(`Processing transaction ${transactionDoc.id}:`, {
            amount: transaction.amount,
            description: transaction.description || "No description",
            type: transaction.type,
            recurring: transaction.recurring,
            nextDueDate: transaction.nextDueDate?.toDate?.()?.toISOString() || "No date",
          })

          try {
            // Create notification message with better formatting
            const notificationTitle = "Transaction Reminder"
            let notificationBody

            if (transaction.description && transaction.description.trim() !== "") {
              notificationBody = `Reminder: ₱${transaction.amount.toFixed(2)} for '${transaction.description}' is due today.`
            } else {
              notificationBody = `Reminder: ₱${transaction.amount.toFixed(2)} ${transaction.type || "transaction"} is due today.`
            }

            // Send FCM notification
            await sendNotification(fcmToken, notificationTitle, notificationBody, transactionDoc.id, userId)
            console.log(`✅ Notification sent to user ${userId} for transaction ${transactionDoc.id}`)
            totalNotificationsSent++

            // Calculate and update next due date
            const currentDueDate = transaction.nextDueDate.toDate()
            const nextDueDate = calculateNextDueDate(currentDueDate, transaction.recurring)

            if (nextDueDate) {
              console.log(`Next due date calculated: ${nextDueDate.toISOString()}`)

              // Update transaction with new next due date
              await transactionDoc.ref.update({
                nextDueDate: nextDueDate,
              })

              console.log(`✅ Updated next due date for transaction ${transactionDoc.id}`)
            } else {
              console.log(`❌ Failed to calculate next due date for transaction ${transactionDoc.id}`)
            }

            // Add to user's notifications collection with all required fields
            await db
              .collection("users")
              .doc(userId)
              .collection("notifications")
              .add({
                title: notificationTitle,
                message: notificationBody,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                lastNotified: admin.firestore.FieldValue.serverTimestamp(),
                nextDueDate: nextDueDate,
                recurring: transaction.recurring,
                iconID: transaction.iconID || 0,
                transactionId: transactionDoc.id,
                isNotified: true,
                isViewed: false,
                type: transaction.type || "Transaction",
              })

            console.log(`✅ Added notification to Firestore for transaction ${transactionDoc.id}`)
          } catch (transactionError) {
            console.error(`❌ Error processing transaction ${transactionDoc.id}:`, transactionError)
            // Continue with other transactions even if one fails
          }
        }
      }

      console.log(`\n=== NOTIFICATION CHECK COMPLETED ===`)
      console.log(`Total users checked: ${usersSnapshot.size}`)
      console.log(`Total transactions processed: ${totalTransactionsProcessed}`)
      console.log(`Total notifications sent: ${totalNotificationsSent}`)

      return {
        success: true,
        usersChecked: usersSnapshot.size,
        transactionsProcessed: totalTransactionsProcessed,
        notificationsSent: totalNotificationsSent,
      }
    } catch (error) {
      console.error("❌ Error in checkRecurringTransactions:", error)
      return {
        success: false,
        error: error.message,
      }
    }
  })

// Enhanced helper function to send FCM notification
async function sendNotification(token, title, body, transactionId, userId) {
  const message = {
    token: token,
    notification: {
      title: title,
      body: body,
    },
    data: {
      title: title,
      body: body,
      transactionId: transactionId,
      userId: userId,
      click_action: "FLUTTER_NOTIFICATION_CLICK",
      type: "transaction_reminder",
    },
    android: {
      notification: {
        icon: "icnotif_transactions",
        color: "#4CAF50",
        channel_id: "transaction_reminders",
        sound: "default",
        priority: "high",
      },
      priority: "high",
    },
    apns: {
      payload: {
        aps: {
          sound: "default",
          badge: 1,
        },
      },
    },
  }

  console.log(`Sending FCM message to user ${userId}:`, {
    token: token.substring(0, 20) + "...",
    title: title,
    body: body.substring(0, 50) + "...",
  })

  try {
    const response = await admin.messaging().send(message)
    console.log(`✅ FCM message sent successfully. Response: ${response}`)
    return response
  } catch (error) {
    console.error(`❌ Error sending FCM message to user ${userId}:`, error)

    // Log specific FCM error codes for debugging
    if (error.code === "messaging/registration-token-not-registered") {
      console.error(`Token is no longer valid for user ${userId} - user may have uninstalled the app`)
    } else if (error.code === "messaging/invalid-registration-token") {
      console.error(`Invalid FCM token format for user ${userId}`)
    } else if (error.code === "messaging/invalid-argument") {
      console.error(`Invalid message format for user ${userId}`)
    }

    throw error
  }
}

// Enhanced helper function to calculate next due date based on recurring type
function calculateNextDueDate(currentDueDate, recurringType) {
  if (!currentDueDate || !recurringType) {
    console.error("Missing currentDueDate or recurringType:", { currentDueDate, recurringType })
    return null
  }

  const date = new Date(currentDueDate)
  console.log(`Calculating next due date from: ${date.toISOString()} for type: ${recurringType}`)

  // Validate the input date
  if (isNaN(date.getTime())) {
    console.error("Invalid date provided:", currentDueDate)
    return null
  }

  switch (recurringType.toLowerCase()) {
    case "daily":
      date.setDate(date.getDate() + 1)
      break
    case "weekly":
      date.setDate(date.getDate() + 7)
      break
    case "monthly":
      date.setMonth(date.getMonth() + 1)
      break
    case "yearly":
      date.setFullYear(date.getFullYear() + 1)
      break
    default:
      console.warn(`Unknown recurring type: ${recurringType}`)
      return null
  }

  console.log(`Next due date calculated: ${date.toISOString()}`)
  return date
}

// Enhanced function to handle token updates
exports.onUserStatusChange = functions.firestore.document("users/{userId}/tokens/fcm").onWrite((change, context) => {
  const userId = context.params.userId

  if (!change.after.exists) {
    console.log(`FCM token deleted for user ${userId}`)
    return null
  }

  const tokenData = change.after.data()
  const newToken = tokenData?.fcmToken

  if (change.before.exists) {
    const oldTokenData = change.before.data()
    const oldToken = oldTokenData?.fcmToken

    if (oldToken !== newToken) {
      console.log(`FCM token updated for user ${userId}`)
      console.log(`Old token: ${oldToken?.substring(0, 20)}...`)
      console.log(`New token: ${newToken?.substring(0, 20)}...`)
    }
  } else {
    console.log(`FCM token created for user ${userId}`)
    console.log(`New token: ${newToken?.substring(0, 20)}...`)
  }

  return null
})

exports.testNotifications = functions.https.onRequest(async (req, res) => {
  console.log("Manual notification test triggered")

  try {
    // Call the main function manually
    const result = await exports.checkRecurringTransactions.run()
    res.json({
      success: true,
      message: "Test completed",
      result: result,
    })
  } catch (error) {
    console.error("Test failed:", error)
    res.status(500).json({
      success: false,
      error: error.message,
    })
  }
})

exports.testSingleNotification = functions.https.onCall(async (data, context) => {
  const userId = context.auth.uid;
  if (!userId) throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');

  const db = admin.firestore();

  try {
    console.log('=== TEST NOTIFICATION FUNCTION CALLED ===');
    console.log('User ID:', userId);

    // Get user's FCM token
    const tokenDoc = await db.collection('users').doc(userId).collection('tokens').doc('fcm').get();

    if (!tokenDoc.exists) {
      console.log('❌ No FCM token document found for user:', userId);
      throw new functions.https.HttpsError('not-found', 'FCM token not found');
    }

    const tokenData = tokenDoc.data();
    const fcmToken = tokenData?.fcmToken;

    if (!fcmToken) {
      console.log('❌ FCM token is empty for user:', userId);
      throw new functions.https.HttpsError('not-found', 'FCM token is empty');
    }

    console.log('✅ FCM token found:', fcmToken.substring(0, 20) + '...');

    // Send test notification
    const message = {
      token: fcmToken,
      notification: {
        title: 'Test Notification',
        body: 'This is a test notification from your Thrifty app!'
      },
      data: {
        type: 'test',
        timestamp: new Date().toISOString()
      },
      android: {
        notification: {
          icon: 'icnotif_transactions',
          color: '#4CAF50',
          channel_id: 'transaction_reminders',
          priority: 'high'
        },
        priority: 'high'
      }
    };

    console.log('📤 Sending test notification...');
    const response = await admin.messaging().send(message);
    console.log('✅ Test notification sent successfully:', response);

    return {
      success: true,
      messageId: response,
      message: 'Test notification sent successfully!'
    };
  } catch (error) {
    console.error('❌ Error sending test notification:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});