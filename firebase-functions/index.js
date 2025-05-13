const functions = require("firebase-functions")
const admin = require("firebase-admin")
admin.initializeApp()

// Cloud function that runs daily to check for due transactions and send notifications
exports.checkRecurringTransactions = functions.pubsub
  .schedule("0 8 * * *")
  .timeZone("Asia/Manila") // Set to Philippines timezone
  .onRun(async (context) => {
    const db = admin.firestore()
    const today = new Date()

    // Set time to beginning of day for consistent comparison
    today.setHours(0, 0, 0, 0)

    // Set time to end of day for query
    const endOfDay = new Date(today)
    endOfDay.setHours(23, 59, 59, 999)

    console.log(`Checking for transactions due between ${today.toISOString()} and ${endOfDay.toISOString()}`)

    try {
      // Query all users
      const usersSnapshot = await db.collection("users").get()

      for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id

        // Find transactions for this user where nextDueDate is today
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

          // Get user's FCM token
          const tokenDoc = await db.collection("users").doc(userId).collection("tokens").doc("fcm").get()

          if (tokenDoc.exists) {
            const fcmToken = tokenDoc.data().fcmToken

            if (fcmToken) {
              // Create notification message
              const notificationTitle = "Transaction Reminder"
              const notificationBody = `Reminder: ₱${transaction.amount.toFixed(2)} for '${transaction.description}' is due today.`

              // Send FCM notification
              await sendNotification(fcmToken, notificationTitle, notificationBody, transactionDoc.id)
              console.log(`Notification sent to user ${userId} for transaction ${transactionDoc.id}`)

              // Calculate and update next due date
              const nextDueDate = calculateNextDueDate(transaction.nextDueDate, transaction.recurring)

              // Update transaction with new next due date
              await transactionDoc.ref.update({
                nextDueDate: nextDueDate,
              })

              console.log(`Updated next due date for transaction ${transactionDoc.id} to ${nextDueDate.toISOString()}`)

              // Add to user's notifications collection
              await db
                .collection("users")
                .doc(userId)
                .collection("notifications")
                .add({
                  title: notificationTitle,
                  message: notificationBody,
                  timestamp: admin.firestore.FieldValue.serverTimestamp(),
                  recurring: transaction.recurring,
                  iconID: transaction.iconID || 0,
                  transactionId: transactionDoc.id,
                })
            }
          }
        }
      }

      console.log("Recurring transaction check completed successfully")
      return null
    } catch (error) {
      console.error("Error checking recurring transactions:", error)
      return null
    }
  })

// Helper function to send FCM notification
async function sendNotification(token, title, body, transactionId) {
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
    },
    android: {
      notification: {
        icon: "icnotif_transactions",
        color: "#4CAF50",
        channel_id: "transaction_reminders",
      },
    },
  }

  try {
    const response = await admin.messaging().send(message)
    console.log("Successfully sent message:", response)
    return response
  } catch (error) {
    console.error("Error sending message:", error)
    throw error
  }
}

// Helper function to calculate next due date based on recurring type
function calculateNextDueDate(currentDueDate, recurringType) {
  const date = new Date(currentDueDate)

  switch (recurringType) {
    case "Daily":
      date.setDate(date.getDate() + 1)
      break
    case "Weekly":
      date.setDate(date.getDate() + 7)
      break
    case "Monthly":
      date.setMonth(date.getMonth() + 1)
      break
    case "Yearly":
      date.setFullYear(date.getFullYear() + 1)
      break
    default:
      // No change for non-recurring transactions
      break
  }

  return date
}

// Function to handle token updates
exports.onUserStatusChange = functions.firestore.document("users/{userId}/tokens/fcm").onWrite((change, context) => {
  // This function will run whenever a user's FCM token is updated
  // You could use this to update subscription topics or perform other actions
  const userId = context.params.userId
  console.log(`FCM token updated for user ${userId}`)
  return null
})
