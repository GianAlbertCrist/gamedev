const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function setAdminRole(email) {
  try {
    const userRecord = await admin.auth().getUserByEmail(email);
    console.log(`Found user: ${userRecord.uid}`);

    await db.collection('users').doc(userRecord.uid).update({
      role: 'admin'
    });

    console.log(`Successfully set admin role for user: ${email}`);
  } catch (error) {
    console.error('Error setting admin role:', error);
  }
}

setAdminRole('darkenborder7@gmail.com')
  .then(() => process.exit(0))
  .catch(error => {
    console.error(error);
    process.exit(1);
  });