package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DynoBg
import com.example.ui.theme.DynoCardBg
import com.example.ui.theme.DynoCardBorder
import com.example.ui.theme.DynoCardSurface
import com.example.ui.theme.DynoPowerCyan
import com.example.ui.theme.DynoTextPrimary
import com.example.ui.theme.DynoTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var useSaeCorrection by remember { mutableStateOf(true) }
    var highSamplingRate by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DynoBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CONFIGURAÇÕES",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DynoTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = DynoTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DynoBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Padrões de Cálculo",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DynoPowerCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Correção SAE J1349", style = MaterialTheme.typography.bodyMedium, color = DynoTextPrimary)
                            Text("Ajusta potência por temperatura e pressão atmosférica", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                        }
                        Switch(
                            checked = useSaeCorrection,
                            onCheckedChange = { useSaeCorrection = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DynoPowerCyan, checkedTrackColor = DynoCardSurface)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amostragem Rápida dos Sensores", style = MaterialTheme.typography.bodyMedium, color = DynoTextPrimary)
                            Text("Aumenta a taxa de atualização para 100Hz", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                        }
                        Switch(
                            checked = highSamplingRate,
                            onCheckedChange = { highSamplingRate = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = DynoPowerCyan, checkedTrackColor = DynoCardSurface)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DynoCardBg),
                border = BorderStroke(1.dp, DynoCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sobre o Dyno Lite",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DynoPowerCyan
                    )
                    Text("Versão 1.0.2 (Build 2 - Commit 4abc266-fix)", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                    Text("Dinamômetro veicular inercial e simulador termodinâmico de alta precisão.", style = MaterialTheme.typography.bodySmall, color = DynoTextSecondary)
                }
            }
        }
    }
}
