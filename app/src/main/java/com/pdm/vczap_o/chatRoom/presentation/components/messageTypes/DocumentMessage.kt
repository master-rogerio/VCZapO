package com.pdm.vczap_o.chatRoom.presentation.components.messageTypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdm.vczap_o.core.model.ChatMessage

@Composable
fun DocumentMessage(
    message: ChatMessage,
    isOwnMessage: Boolean,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determinar ícone baseado no tipo de arquivo
    val fileIcon = getFileIcon(message.fileName ?: "")

    Card(
        modifier = modifier
            .widthIn(min = 200.dp, max = 300.dp)
            .clickable {
                // TODO: Implementar download/abertura do documento
                // Pode abrir o documento ou iniciar download
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOwnMessage)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do arquivo
            Icon(
                imageVector = fileIcon,
                contentDescription = "Document",
                tint = if (isOwnMessage)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Nome do arquivo
                Text(
                    text = message.fileName ?: "Documento",
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Legenda (se houver)
                if (message.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.content,
                        fontSize = (fontSize - 2).sp,
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Indicador de documento
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "📄 Document",
                    fontSize = (fontSize - 3).sp,
                    color = if (isOwnMessage)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun getFileIcon(fileName: String): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()

    return when (extension) {
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        "txt" -> Icons.Default.TextSnippet
        "zip", "rar" -> Icons.Default.Archive
        "jpg", "jpeg", "png", "gif" -> Icons.Default.Image
        "mp4", "avi", "mov" -> Icons.Default.VideoFile
        "mp3", "wav", "aac" -> Icons.Default.AudioFile
        else -> Icons.Default.InsertDriveFile
    }
}