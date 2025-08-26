package com.pdm.vczap_o.auth.presentation.components

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdm.vczap_o.auth.presentation.viewmodels.AuthViewModel
import com.pdm.vczap_o.core.domain.showToast
import kotlinx.coroutines.DelicateCoroutinesApi

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun AuthForm(
    isLogin: Boolean, onToggleMode: () -> Unit, authViewModel: AuthViewModel,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val context = LocalContext.current
    val fieldShape = RoundedCornerShape(20.dp)

    val focusManager = LocalFocusManager.current
    val (passwordFocusRequester, confirmPasswordFocusRequester) = remember { FocusRequester.createRefs() }

    val isFormValid by remember(email, password) {
        derivedStateOf {
            val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
            val isPasswordValid = password.length in 6..20
            isEmailValid && isPasswordValid
            if (isLogin) {
                isEmailValid && isPasswordValid
            } else {
                isEmailValid && isPasswordValid && password == confirmPassword
            }
        }
    }

    val submitAction = {
        focusManager.clearFocus()
        if (isFormValid) {
            if (isLogin) {
                authViewModel.login(email, password)
            } else {
                authViewModel.signUp(email, password)
            }
        } else {
            // Mostra um erro se o formulário for inválido ao tentar submeter
            if (password != confirmPassword && !isLogin) {
                showToast(context, "As senhas não coincidem")
            } else {
                showToast(context, "Por favor, preencha os campos corretamente")
            }
        }
    }


    Column(
        modifier = Modifier.fillMaxWidth(0.85f), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onNext = {
                passwordFocusRequester.requestFocus()
            }),
            singleLine = true,
            shape = fieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Senha")
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isLogin) ImeAction.Done else ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(
                onNext = { confirmPasswordFocusRequester.requestFocus() }, // Ação para Registro
                onDone = { submitAction() }                                // Ação para Login
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Mostrar Senha" else "Esconder Senha"
                    )
                }
            },
            singleLine = true,
            shape = fieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentType = if (isLogin) ContentType.Password else ContentType.NewPassword
                }
                .focusRequester(passwordFocusRequester)
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(visible = !isLogin) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirme sua Senha") },
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitAction() } // Ação para Registro
                ),
                singleLine = true,
                shape = fieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentType = ContentType.NewPassword }
                    .focusRequester(confirmPasswordFocusRequester),
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = "Confirme sua Senha")
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Esqueceu sua Senha?", modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    if (email.isNotBlank()) {
                        authViewModel.resetPassword(email)
                    } else {
                        Toast.makeText(
                            context, "Insira seu endereço de Email", Toast.LENGTH_LONG
                        ).show()
                    }
                }, color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {submitAction() },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            shape = fieldShape,
            enabled = isFormValid && !isLoggingIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.align(
                        Alignment.CenterVertically
                    )
                )
            } else {
                Text(text = if (isLogin) "Login" else "Registro", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLogin) "Não tem uma conta? Registre-se" else "Já possui uma conta? Login",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onToggleMode() })
    }
}
