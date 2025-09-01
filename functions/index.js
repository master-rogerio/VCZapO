const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.sendNotification = functions.https.onCall(async (data, context) => {
  try {
    console.log('🔥 Function iniciada');
    console.log('📦 Dados RAW recebidos:', JSON.stringify(data, null, 2));
    console.log('📦 Tipo de data:', typeof data);
    console.log('📦 Keys do data:', Object.keys(data || {}));
    
    // DEBUG: Verificar cada campo antes da conversão
    console.log('🔍 ANTES da conversão:');
    console.log('  - data.recipientUserId:', data.recipientUserId, '(type:', typeof data.recipientUserId, ')');
    console.log('  - data.senderUserId:', data.senderUserId, '(type:', typeof data.senderUserId, ')');
    console.log('  - data.roomId:', data.roomId, '(type:', typeof data.roomId, ')');
    
    // Extrair e validar dados de forma segura
    const recipientUserId = String(data.recipientUserId || '').trim();
    const title = String(data.title || 'Nova mensagem');
    const body = String(data.body || 'Você recebeu uma mensagem');
    const roomId = String(data.roomId || '');
    const senderUserId = String(data.senderUserId || '');
    const profileUrl = String(data.profileUrl || '');
    
    console.log('📋 APÓS conversão:');
    console.log('  - recipientUserId:', `"${recipientUserId}"`, 'length:', recipientUserId.length);
    console.log('  - senderUserId:', `"${senderUserId}"`, 'length:', senderUserId.length);
    console.log('  - title:', title);
    console.log('  - body:', body);
    console.log('  - roomId:', roomId);

    // Validação básica
    if (!recipientUserId || recipientUserId.length === 0) {
      console.log('❌ recipientUserId inválido');
      return { success: false, error: 'recipientUserId é obrigatório' };
    }

    if (!senderUserId || senderUserId.length === 0) {
      console.log('❌ senderUserId inválido');
      return { success: false, error: 'senderUserId é obrigatório' };
    }

    console.log('✅ Dados validados com sucesso');

    // Buscar usuário no Firestore
    console.log('🔍 Buscando usuário no Firestore...');
    
    const userDoc = await admin.firestore()
        .collection('users')
        .doc(recipientUserId)
        .get();

    console.log('📄 Documento existe?', userDoc.exists);

    if (!userDoc.exists) {
      console.log('❌ Usuário não encontrado');
      return { success: false, error: 'Usuário não encontrado' };
    }

    const userData = userDoc.data();
    const token = userData?.deviceToken;
    
    console.log('🎯 Token existe?', !!token);
    console.log('🎯 Token length:', token ? token.length : 0);

    if (!token || token.length < 50) {
      console.log('❌ Token FCM inválido ou não encontrado');
      return { success: false, error: 'Token FCM não encontrado ou inválido' };
    }

    console.log('📤 Preparando mensagem FCM...');

    // Criar mensagem FCM de forma segura
    const message = {
      token: token,
      notification: {
        title: title,
        body: body,
      },
      data: {
        roomId: roomId,
        senderUserId: senderUserId,
        recipientUserId: recipientUserId,
        profileUrl: profileUrl,
        type: 'chat_message',
        timestamp: Date.now().toString()
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'flash',
          priority: 'high',
          defaultSound: true,
          defaultVibrateTimings: true
        }
      },
      apns: {
        payload: {
          aps: {
            alert: {
              title: title,
              body: body
            },
            sound: 'default',
            badge: 1
          }
        }
      }
    };

    console.log('🚀 Enviando via FCM...');
    console.log('📤 Token destino:', token.substring(0, 20) + '...');

    const response = await admin.messaging().send(message);

    console.log('✅ FCM enviado com sucesso');
    console.log('📨 Response ID:', response);

    return { 
      success: true, 
      messageId: response,
      timestamp: Date.now()
    };

  } catch (error) {
    console.error('❌ Erro na function:', error.message);
    console.error('❌ Error code:', error.code);
    
    // Retornar erro de forma segura sem referências circulares
    return { 
      success: false, 
      error: error.message || 'Erro desconhecido',
      code: error.code || 'UNKNOWN_ERROR',
      timestamp: Date.now()
    };
  }
});