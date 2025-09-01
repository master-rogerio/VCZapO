  const functions = require('firebase-functions');                                                                     
   const admin = require('firebase-admin');                                                                             
                                                                                                                        
   admin.initializeApp();                                                                                               
                                                                                                                        
   exports.sendNotification = functions.https.onCall(async (data, context) => {                                         
     try {                                                                                                              
       const {recipientUserId, title, body, roomId, senderUserId} = data;                                               
                                                                                                                        
       const userDoc = await admin.firestore()                                                                          
           .collection('users')                                                                                         
           .doc(recipientUserId)                                                                                        
           .get();                                                                                                      
                                                                                                                        
       if (!userDoc.exists) {                                                                                           
         throw new Error('Usuário não encontrado');                                                                     
       }                                                                                                                
                                                                                                                        
       const userData = userDoc.data();                                                                                 
       const token = userData && userData.deviceToken;                                                                  
                                                                                                                        
       if (!token) {                                                                                                    
         throw new Error('Token não encontrado');                                                                       
       }                                                                                                                
                                                                                                                        
       const message = {                                                                                                
         token: token,                                                                                                  
         notification: {                                                                                                
           title: title,                                                                                                
           body: body,                                                                                                  
         },                                                                                                             
         data: {                                                                                                        
           roomId: roomId,                                                                                              
           senderUserId: senderUserId,                                                                                  
         },                                                                                                             
       };                                                                                                               
                                                                                                                        
       const response = await admin.messaging().send(message);                                                          
       return {success: true, messageId: response};                                                                     
                                                                                                                        
     } catch (error) {                                                                                                  
       throw new functions.https.HttpsError('internal', error.message);                                                 
     }                                                                                                                  
   }); 