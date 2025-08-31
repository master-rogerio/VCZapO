package com.pdm.vczap_o.chatRoom.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.pdm.vczap_o.R
import com.pdm.vczap_o.core.model.User
import com.pdm.vczap_o.navigation.OtherProfileScreenDC
import com.google.gson.Gson

@Composable
fun HeaderBar(
    name: String,
    netActivity: String,
    pic: String?,
    goBack: () -> Unit,
    userData: User,
    navController: NavController,
    chatOptionsList: List<DropMenu>,
    onImageClick: () -> Unit,
    // ADICIONADO: Parâmetros para status online e digitação
    isUserOnline: Boolean = false,
    isUserTyping: Boolean = false,
    lastSeen: String? = null,
    // FIM ADICIONADO
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
//            Back button, profile pic and name/network status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "back button",
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(25.dp)
                    .clickable(onClick = goBack),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(5.dp))
//                profile pic and name/network status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = {
                        val userJson = Gson().toJson(userData)
                        navController.navigate(OtherProfileScreenDC(userJson))
                    })
                    .weight(1f)
            ) {
                AsyncImage(
                    model = pic,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(45.dp)
                        .align(Alignment.CenterVertically),
                    contentScale = ContentScale.Crop,
                    error = rememberAsyncImagePainter(R.drawable.person)
                )

                Column {
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 10.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    // ADICIONADO: Lógica para exibir status do usuário
                    // DEBUG TEMPORÁRIO
                    android.util.Log.d("TopBar", "=== TOPBAR DEBUG ===")
                    android.util.Log.d("TopBar", "isUserTyping: $isUserTyping")
                    android.util.Log.d("TopBar", "isUserOnline: $isUserOnline")
                    android.util.Log.d("TopBar", "lastSeen: $lastSeen")
                    android.util.Log.d("TopBar", "netActivity: '$netActivity'")
                    
                    // SIMPLIFICADO: Lógica mais clara
                    val statusText = if (isUserTyping) {
                        android.util.Log.d("TopBar", "ESCOLHEU: digitando...")
                        "digitando..."
                    } else if (isUserOnline) {
                        android.util.Log.d("TopBar", "ESCOLHEU: online")
                        "online"
                    } else if (!lastSeen.isNullOrBlank()) {
                        android.util.Log.d("TopBar", "ESCOLHEU: visto por último $lastSeen")
                        "visto por último $lastSeen"
                    } else if (netActivity.isNotEmpty()) {
                        android.util.Log.d("TopBar", "ESCOLHEU: netActivity $netActivity")
                        netActivity
                    } else {
                        android.util.Log.d("TopBar", "ESCOLHEU: vazio")
                        ""
                    }
                    
                    android.util.Log.d("TopBar", "STATUS FINAL: '$statusText'")
                    android.util.Log.d("TopBar", "=== FIM TOPBAR ===")
                    
                    val statusColor = when {
                        isUserTyping -> MaterialTheme.colorScheme.primary
                        isUserOnline -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    }
                    
                    if (statusText.isNotEmpty()) {
                        Text(
                            text = statusText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 11.dp),
                            color = statusColor
                        )
                    }
                    // FIM ADICIONADO
                }
            }

        }

//          Call and more vert icon buttons
        Row(modifier = Modifier.padding(end = 12.dp)) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = { onImageClick() })
            )
            Spacer(modifier = Modifier.width(15.dp))
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clickable(onClick = { expanded = !expanded })
            )
            PopUpMenu(
                expanded = expanded, { expanded = !expanded },
                modifier = Modifier,
                dropItems = chatOptionsList,
                reactions = {}
            )
        }
    }
}
