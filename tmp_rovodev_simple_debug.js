// VERSÃO SIMPLES SEM CIRCULAR REFERENCE - Cole no functions/index.js

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.sendNotification = functions.https.onCall(async (data, context) => {
  try {
    console.log('🔥 Function iniciada');
    
    // Extrair dados de forma segura
    const recipientUserId = data.recipientUserId;
    const title = data.title;
    const body = data.body;
    const roomId = data.roomId;
    const senderUserId = data.senderUserId;
    
    console.log('📋 recipientUserId:', recipientUserId);
    console.log('📋 recipientUserId type:', typeof recipientUserId);
    console.log('📋 recipientUserId length:', recipientUserId ? recipientUserId.length : 'undefined');
    
    // Validação básica
    if (!recipientUserId) {
      console.log('❌ recipientUserId é falsy');
      return {success: false, error: 'recipientUserId é obrigatório'};
    }
    
    if (typeof recipientUserId !== 'string') {
      console.log('❌ recipientUserId não é string');
      return {success: false, error: 'recipientUserId deve ser string'};
    }
    
    if (recipientUserId.trim().length === 0) {
      console.log('❌ recipientUserId está vazio após trim');
      return {success: false, error: 'recipientUserId está vazio'};
    }
    
    console.log('✅ recipientUserId válido:', recipientUserId);
    
    // Tentar acessar Firestore
    console.log('🔍 Buscando usuário no Firestore...');
    
    const userDoc = await admin.firestore()
        .collection('users')
        .doc(recipientUserId)
        .get();
    
    console.log('📄 Documento existe?', userDoc.exists);
    
    if (!userDoc.exists) {
      console.log('❌ Usuário não encontrado');
      return {success: false, error: 'Usuário não encontrado'};
    }
    
    const userData = userDoc.data();
    console.log('📋 userData existe?', !!userData);
    
    const token = userData ? userData.deviceToken : null;
    console.log('🎯 Token existe?', !!token);
    
    if (!token) {
      console.log('❌ Token não encontrado');
      return {success: false, error: 'Token FCM não encontrado'};
    }
    
    console.log('📤 Preparando mensagem FCM...');
    
    const message = {
      token: token,
      notification: {
        title: title || 'Nova mensagem',
        body: body || 'Você recebeu uma mensagem',
      },
      data: {
        roomId: roomId || '',
        senderUserId: senderUserId || '',
      },
    };
    
    console.log('🚀 Enviando via FCM...');
    
    const response = await admin.messaging().send(message);
    
    console.log('✅ FCM enviado com sucesso');
    console.log('📨 Response ID:', response);
    
    return {success: true, messageId: response};

  } catch (error) {
    console.error('❌ Erro na function:', error.message);
    console.error('❌ Error code:', error.code);
    return {success: false, error: error.message};
  }
});