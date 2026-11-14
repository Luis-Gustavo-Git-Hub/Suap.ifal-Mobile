package com.example.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AppTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false, label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null, leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null, supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false, visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE, minLines: Int = 1,
    shape: Shape = MaterialTheme.shapes.small, colors: TextFieldColors = OutlinedTextFieldDefaults.colors(), password: Boolean = false) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled, readOnly = readOnly, label = label, placeholder = placeholder, leadingIcon = leadingIcon,
        trailingIcon = if (password) { { IconButton(onClick = { visible = !visible }) {
            Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (visible) "Ocultar senha" else "Mostrar senha")
        } } } else trailingIcon,
        supportingText = supportingText, isError = isError, visualTransformation = if (password && !visible) PasswordVisualTransformation() else visualTransformation,
        keyboardOptions = keyboardOptions, singleLine = singleLine, maxLines = maxLines, minLines = minLines, shape = shape, colors = colors)
}
