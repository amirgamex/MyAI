package com.myai.app.ui.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myai.app.data.db.MemoryEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    viewModel: MemoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت حافظه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    if (uiState.memories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllMemories() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "پاک کردن همه"
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.startAdd() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن حافظه"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.memories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "هنوز حافظه‌ای ذخیره نشده",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "می‌توانی اطلاعات مهم را اینجا ذخیره کنی تا دستیار به خاطر بسپارد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.startAdd() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("افزودن حافظه")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.memories, key = { it.id }) { memory ->
                        MemoryItem(
                            memory = memory,
                            onEdit = { viewModel.startEdit(memory) },
                            onDelete = { viewModel.deleteMemory(memory.id) }
                        )
                    }
                }
            }
        }
    }

    // دیالوگ افزودن / ویرایش
    if (uiState.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = {
                Text(if (uiState.editingId == null) "افزودن حافظه" else "ویرایش حافظه")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.editKey,
                        onValueChange = viewModel::onKeyChange,
                        label = { Text("عنوان / کلید") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("مثلاً: نام من") }
                    )
                    OutlinedTextField(
                        value = uiState.editValue,
                        onValueChange = viewModel::onValueChange,
                        label = { Text("مقدار") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("مثلاً: علی") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveMemory() },
                    enabled = uiState.editKey.isNotBlank() && uiState.editValue.isNotBlank()
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEdit() }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun MemoryItem(
    memory: MemoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memory.key,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = memory.value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "آخرین بروزرسانی: ${dateFormat.format(Date(memory.updatedAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
