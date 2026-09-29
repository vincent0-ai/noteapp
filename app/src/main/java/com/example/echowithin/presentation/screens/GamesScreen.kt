package com.example.echowithin.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.ui.theme.BrandOrange
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

enum class GameCategory(val label: String) {
    ARCADE("Arcade & 1v1"),
    PARTY("Party & Trivia"),
    COOP("Couples & Co-op")
}

fun launchInAppGameTab(context: Context, url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(android.graphics.Color.parseColor("#18191d"))
                    .setNavigationBarColor(android.graphics.Color.parseColor("#121316"))
                    .build()
            )
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var pinText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GameCategory.ARCADE) }
    var selectedArcadeGame by remember { mutableStateOf("tictactoe") } // "tictactoe", "connectfour", "snake"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { EchoWithinTopBarTitle() },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Games & Party Lobbies",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Play instant arcade games or join live party lobbies with friends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick PIN Join Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Join Room by PIN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = pinText,
                                onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) pinText = it },
                                placeholder = { Text("6-digit PIN", style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (pinText.length == 6) {
                                        launchInAppGameTab(context, "https://echowithin.xyz/games/join?pin=$pinText")
                                    }
                                })
                            )
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (pinText.length == 6) {
                                        launchInAppGameTab(context, "https://echowithin.xyz/games/join?pin=$pinText")
                                    }
                                },
                                enabled = pinText.length == 6,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                            ) {
                                Text("Join")
                            }
                        }
                    }
                }
            }

            // Category Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GameCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Arcade Category Content
            if (selectedCategory == GameCategory.ARCADE) {
                // Game Selector Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedArcadeGame == "tictactoe",
                            onClick = { selectedArcadeGame = "tictactoe" },
                            label = { Text("Tic-Tac-Toe") }
                        )
                        FilterChip(
                            selected = selectedArcadeGame == "connectfour",
                            onClick = { selectedArcadeGame = "connectfour" },
                            label = { Text("Connect Four") }
                        )
                        FilterChip(
                            selected = selectedArcadeGame == "snake",
                            onClick = { selectedArcadeGame = "snake" },
                            label = { Text("Snake") }
                        )
                    }
                }

                // Interactive Game View
                item {
                    when (selectedArcadeGame) {
                        "tictactoe" -> TicTacToeView()
                        "connectfour" -> ConnectFourView()
                        "snake" -> SnakeGameView()
                    }
                }

                // Other Arcade Web Links
                item {
                    Text(
                        text = "More Arcade Classics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArcadeGameLinkCard(
                            title = "Floppy Bird",
                            description = "Tap to flap and fly through pipe obstacles to beat your high score.",
                            url = "https://echowithin.xyz/games/floppy_bird"
                        )
                        ArcadeGameLinkCard(
                            title = "Slime Volleyball",
                            description = "Fast physics arcade volleyball with bouncy slime controls.",
                            url = "https://echowithin.xyz/games/slime_volleyball"
                        )
                        ArcadeGameLinkCard(
                            title = "Dots and Boxes",
                            description = "Strategic line-connecting grid game to capture territory.",
                            url = "https://echowithin.xyz/games/dots_and_boxes"
                        )
                        ArcadeGameLinkCard(
                            title = "Ping Pong",
                            description = "Retro paddle rally game with smooth touch tracking.",
                            url = "https://echowithin.xyz/games/ping_pong"
                        )
                    }
                }
            }

            // Party Category Content
            if (selectedCategory == GameCategory.PARTY) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Party & Social Rooms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = {
                                launchInAppGameTab(context, "https://echowithin.xyz/games/create")
                            }
                        ) {
                            Text("+ Host Room", color = BrandOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PartyRoomCard(
                            title = "Trivia Challenge",
                            badge = "Live Show",
                            description = "Host live multiplayer game shows with instant PIN entry, timed buzzer questions, speed streaks, and podium celebrations.",
                            createType = "trivia"
                        )
                        PartyRoomCard(
                            title = "Interactive Polls",
                            badge = "Live Voting",
                            description = "Create real-time polls, question rooms, and community consensus boards with live bar charts.",
                            createType = "poll"
                        )
                        PartyRoomCard(
                            title = "Would You Rather",
                            badge = "Social",
                            description = "Tough dilemmas with split votes, group statistics, and lively debates.",
                            createType = "wyr"
                        )
                        PartyRoomCard(
                            title = "Two Truths & A Lie",
                            badge = "Deception",
                            description = "Guess which statement is a fabrication and deceive friends.",
                            createType = "ttal"
                        )
                        PartyRoomCard(
                            title = "Collaborative Story",
                            badge = "Creative",
                            description = "Chain-writing where players take turns adding one sentence to craft humorous stories.",
                            createType = "story"
                        )
                        PartyRoomCard(
                            title = "Caption This",
                            badge = "Creativity",
                            description = "Share quirky scenarios or prompts and vote on who writes the funniest, cleverest one-liner caption.",
                            createType = "caption"
                        )
                    }
                }
            }

            // Couples & Co-op Category Content
            if (selectedCategory == GameCategory.COOP) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Couples & Co-op Games",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = {
                                launchInAppGameTab(context, "https://echowithin.xyz/games/create?type=duet")
                            }
                        ) {
                            Text("+ Host Duo", color = BrandOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArcadeGameLinkCard(
                            title = "Word Bond: Duet",
                            description = "Two-player cooperative word deduction puzzle. Give one-word clues to contact all partner words without striking the assassin.",
                            url = "https://echowithin.xyz/games/word-duet"
                        )
                        ArcadeGameLinkCard(
                            title = "Team Crossword",
                            description = "Collaborative live crossword grid with synchronized cursor tracking, partner hints, and shared solving timer.",
                            url = "https://echowithin.xyz/games/crossword"
                        )
                    }
                }
            }
        }
    }
}

// ── Native Game: Tic-Tac-Toe ──────────────────────────────────────

enum class TicTacToeDifficulty(val label: String, val badge: String) {
    EASY("Easy", "Casual"),
    MEDIUM("Medium", "Balanced"),
    HARD("Hard", "Unbeatable")
}

private val TTT_WINNING_LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
    listOf(0, 4, 8), listOf(2, 4, 6)                   // Diagonals
)

private fun checkTttWinner(b: List<String>): Pair<String?, List<Int>?> {
    for (line in TTT_WINNING_LINES) {
        val (a, c1, c2) = line
        if (b[a].isNotEmpty() && b[a] == b[c1] && b[a] == b[c2]) {
            return Pair(b[a], line)
        }
    }
    if (b.all { it.isNotEmpty() }) return Pair("Tie", null)
    return Pair(null, null)
}

private fun checkTttWinForSymbol(sym: String, b: List<String>): Boolean {
    return TTT_WINNING_LINES.any { (a, c1, c2) ->
        b[a] == sym && b[c1] == sym && b[c2] == sym
    }
}

private fun findTttWinningMove(sym: String, b: List<String>): Int? {
    for (i in b.indices) {
        if (b[i].isEmpty()) {
            val copy = b.toMutableList()
            copy[i] = sym
            if (checkTttWinForSymbol(sym, copy)) return i
        }
    }
    return null
}

private fun findTttForkMove(sym: String, b: List<String>): Int? {
    val empty = b.indices.filter { b[it].isEmpty() }
    if (empty.size < 5) return null
    for (idx in empty) {
        val copy = b.toMutableList()
        copy[idx] = sym
        var winCount = 0
        for (nextIdx in copy.indices.filter { copy[it].isEmpty() }) {
            copy[nextIdx] = sym
            if (checkTttWinForSymbol(sym, copy)) winCount++
            copy[nextIdx] = ""
        }
        if (winCount >= 2) return idx
    }
    return null
}

private fun getTttCpuMove(
    board: List<String>,
    difficulty: TicTacToeDifficulty,
    cpuSym: String = "O",
    playerSym: String = "X"
): Int? {
    val empty = board.indices.filter { board[it].isEmpty() }
    if (empty.isEmpty()) return null

    when (difficulty) {
        TicTacToeDifficulty.EASY -> {
            // 30% chance to block or win, otherwise random
            if (Random.nextFloat() < 0.3f) {
                val win = findTttWinningMove(cpuSym, board)
                if (win != null) return win
                val block = findTttWinningMove(playerSym, board)
                if (block != null) return block
            }
            return empty.random()
        }
        TicTacToeDifficulty.MEDIUM -> {
            // 20% mistake chance
            if (Random.nextFloat() > 0.2f) {
                val win = findTttWinningMove(cpuSym, board)
                if (win != null) return win
                val block = findTttWinningMove(playerSym, board)
                if (block != null) return block
                if (board[4].isEmpty()) return 4
            }
            return empty.random()
        }
        TicTacToeDifficulty.HARD -> {
            // Unbeatable Optimal Strategy (matching web tic_tac_toe.js)
            // 1. Immediate winning move
            val win = findTttWinningMove(cpuSym, board)
            if (win != null) return win

            // 2. Block player's immediate win
            val block = findTttWinningMove(playerSym, board)
            if (block != null) return block

            // 3. Create fork
            val fork = findTttForkMove(cpuSym, board)
            if (fork != null) return fork

            // 4. Block player's fork
            val blockFork = findTttForkMove(playerSym, board)
            if (blockFork != null) return blockFork

            // 5. Center
            if (board[4].isEmpty()) return 4

            // 6. Opposite corner
            val corners = listOf(0, 2, 6, 8)
            val playerCorners = corners.filter { board[it] == playerSym }
            if (playerCorners.size == 1) {
                val opp = mapOf(0 to 8, 2 to 6, 6 to 2, 8 to 0)
                val oppCorner = opp[playerCorners[0]]
                if (oppCorner != null && board[oppCorner].isEmpty()) return oppCorner
            }

            // 7. Any empty corner
            val emptyCorners = corners.filter { board[it].isEmpty() }
            if (emptyCorners.isNotEmpty()) {
                return emptyCorners.random()
            }

            // 8. Remaining empty sides
            return empty.random()
        }
    }
}

@Composable
fun TicTacToeView() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("echowithin_games_tictactoe", Context.MODE_PRIVATE) }

    var board by remember { mutableStateOf(List(9) { "" }) }
    var isXTurn by remember { mutableStateOf(true) }
    var isAiMode by remember { mutableStateOf(true) }
    var difficulty by remember { mutableStateOf(TicTacToeDifficulty.HARD) }
    var isAiThinking by remember { mutableStateOf(false) }

    var xWins by remember { mutableStateOf(prefs.getInt("x_wins", 0)) }
    var oWins by remember { mutableStateOf(prefs.getInt("o_wins", 0)) }
    var ties by remember { mutableStateOf(prefs.getInt("ties", 0)) }

    val (winner, winningLine) = checkTttWinner(board)

    LaunchedEffect(isXTurn, isAiMode, winner, difficulty) {
        if (isAiMode && !isXTurn && winner == null) {
            isAiThinking = true
            val delayMs = when (difficulty) {
                TicTacToeDifficulty.EASY -> 380L
                TicTacToeDifficulty.MEDIUM -> 500L
                TicTacToeDifficulty.HARD -> 620L
            }
            delay(delayMs)
            val move = getTttCpuMove(board, difficulty, cpuSym = "O", playerSym = "X")
            if (move != null) {
                val newBoard = board.toMutableList()
                newBoard[move] = "O"
                board = newBoard
                isXTurn = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            isAiThinking = false
        }
    }

    LaunchedEffect(winner) {
        when (winner) {
            "X" -> {
                xWins++
                prefs.edit().putInt("x_wins", xWins).apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            "O" -> {
                oWins++
                prefs.edit().putInt("o_wins", oWins).apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            "Tie" -> {
                ties++
                prefs.edit().putInt("ties", ties).apply()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tic-Tac-Toe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isAiMode,
                        onClick = {
                            isAiMode = true
                            board = List(9) { "" }
                            isXTurn = true
                        },
                        label = { Text("VS AI") }
                    )
                    FilterChip(
                        selected = !isAiMode,
                        onClick = {
                            isAiMode = false
                            board = List(9) { "" }
                            isXTurn = true
                        },
                        label = { Text("2P") }
                    )
                }
            }

            // Difficulty Chips (Visible when VS AI is active)
            AnimatedVisibility(visible = isAiMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Difficulty:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TicTacToeDifficulty.entries.forEach { diff ->
                        val isSelected = difficulty == diff
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (difficulty != diff) {
                                    difficulty = diff
                                    board = List(9) { "" }
                                    isXTurn = true
                                }
                            },
                            label = { Text(diff.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            // Scoreboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("X (You): $xWins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandOrange)
                Text("Ties: $ties", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (isAiMode) "O (AI): $oWins" else "O: $oWins",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Status Banner
            val statusText = when {
                winner == "X" -> if (isAiMode) "🎉 You Win!" else "🎉 Player X Wins!"
                winner == "O" -> if (isAiMode) "🤖 AI (${difficulty.label}) Wins!" else "🎉 Player O Wins!"
                winner == "Tie" -> "🤝 It's a Tie!"
                isAiThinking -> "🤖 AI is thinking..."
                isXTurn -> if (isAiMode) "Your Turn (X)" else "Turn: X"
                else -> if (isAiMode) "AI's Turn (O)..." else "Turn: O"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    winner == "X" -> BrandOrange
                    winner == "O" -> MaterialTheme.colorScheme.primary
                    winner == "Tie" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // 3x3 Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0..2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0..2) {
                            val idx = row * 3 + col
                            val cell = board[idx]
                            val isWinningCell = winningLine?.contains(idx) == true

                            val cellBg = when {
                                isWinningCell && winner == "X" -> BrandOrange.copy(alpha = 0.25f)
                                isWinningCell && winner == "O" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }

                            val cellBorder = when {
                                isWinningCell && winner == "X" -> BorderStroke(2.dp, BrandOrange)
                                isWinningCell && winner == "O" -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }

                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellBg)
                                    .border(cellBorder, RoundedCornerShape(10.dp))
                                    .clickable(enabled = cell.isEmpty() && winner == null && (!isAiMode || (isXTurn && !isAiThinking))) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val newBoard = board.toMutableList()
                                        newBoard[idx] = if (isXTurn) "X" else "O"
                                        board = newBoard
                                        isXTurn = !isXTurn
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell,
                                    fontSize = if (isWinningCell) 32.sp else 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (cell == "X") BrandOrange else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Buttons: Play Again & Reset Score
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        board = List(9) { "" }
                        isXTurn = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Play Again", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Again", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }

                if (xWins > 0 || oWins > 0 || ties > 0) {
                    OutlinedButton(
                        onClick = {
                            xWins = 0
                            oWins = 0
                            ties = 0
                            prefs.edit().clear().apply()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Reset Scores", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ── Native Game: Connect Four ─────────────────────────────────────

enum class ConnectFourDifficulty(val label: String) {
    EASY("Easy"),
    NORMAL("Smart")
}

private const val C4_ROWS = 6
private const val C4_COLS = 7

private fun getC4DropRow(col: Int, b: List<Int>): Int {
    for (r in C4_ROWS - 1 downTo 0) {
        if (b[r * C4_COLS + col] == 0) return r
    }
    return -1
}

private fun checkConnectFourWin(b: List<Int>): Pair<Int?, List<Int>?> {
    fun get(r: Int, c: Int) = if (r in 0 until C4_ROWS && c in 0 until C4_COLS) b[r * C4_COLS + c] else 0
    fun idx(r: Int, c: Int) = r * C4_COLS + c

    for (r in 0 until C4_ROWS) {
        for (c in 0 until C4_COLS) {
            val p = get(r, c)
            if (p == 0) continue
            // Horizontal
            if (c + 3 < C4_COLS && get(r, c + 1) == p && get(r, c + 2) == p && get(r, c + 3) == p) {
                return Pair(p, listOf(idx(r, c), idx(r, c + 1), idx(r, c + 2), idx(r, c + 3)))
            }
            // Vertical
            if (r + 3 < C4_ROWS && get(r + 1, c) == p && get(r + 2, c) == p && get(r + 3, c) == p) {
                return Pair(p, listOf(idx(r, c), idx(r + 1, c), idx(r + 2, c), idx(r + 3, c)))
            }
            // Diagonal \
            if (r + 3 < C4_ROWS && c + 3 < C4_COLS && get(r + 1, c + 1) == p && get(r + 2, c + 2) == p && get(r + 3, c + 3) == p) {
                return Pair(p, listOf(idx(r, c), idx(r + 1, c + 1), idx(r + 2, c + 2), idx(r + 3, c + 3)))
            }
            // Diagonal /
            if (r - 3 >= 0 && c + 3 < C4_COLS && get(r - 1, c + 1) == p && get(r - 2, c + 2) == p && get(r - 3, c + 3) == p) {
                return Pair(p, listOf(idx(r, c), idx(r - 1, c + 1), idx(r - 2, c + 2), idx(r - 3, c + 3)))
            }
        }
    }
    if (b.all { it != 0 }) return Pair(0, null) // Draw
    return Pair(null, null)
}

private fun checkC4MoveWins(r: Int, c: Int, p: Int, b: List<Int>): Boolean {
    val copy = b.toMutableList()
    copy[r * C4_COLS + c] = p
    val (win, _) = checkConnectFourWin(copy)
    return win == p
}

private fun getConnectFourCpuColumn(b: List<Int>, difficulty: ConnectFourDifficulty): Int {
    val validCols = (0 until C4_COLS).filter { getC4DropRow(it, b) != -1 }
    if (validCols.isEmpty()) return -1

    // 1. Can CPU (player 2) win immediately?
    for (c in validCols) {
        val r = getC4DropRow(c, b)
        if (checkC4MoveWins(r, c, 2, b)) return c
    }

    // 2. Can Player 1 win immediately? Block it!
    for (c in validCols) {
        val r = getC4DropRow(c, b)
        if (checkC4MoveWins(r, c, 1, b)) return c
    }

    if (difficulty == ConnectFourDifficulty.EASY) {
        return validCols.random()
    }

    // Smart: Center bias and avoid handing player a win directly on top
    val preferredCols = listOf(3, 2, 4, 1, 5, 0, 6).filter { it in validCols }
    val safeCols = preferredCols.filter { c ->
        val r = getC4DropRow(c, b)
        if (r > 0) !checkC4MoveWins(r - 1, c, 1, b) else true
    }
    val candidates = if (safeCols.isNotEmpty()) safeCols else preferredCols
    return candidates.firstOrNull() ?: validCols.random()
}

@Composable
fun ConnectFourView() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("echowithin_games_c4", Context.MODE_PRIVATE) }

    var board by remember { mutableStateOf(List(C4_ROWS * C4_COLS) { 0 }) }
    var turn by remember { mutableStateOf(1) } // 1=Orange (Player 1), 2=Blue (Player 2 or AI)
    var isAiMode by remember { mutableStateOf(true) }
    var difficulty by remember { mutableStateOf(ConnectFourDifficulty.NORMAL) }
    var isAiThinking by remember { mutableStateOf(false) }

    var p1Wins by remember { mutableStateOf(prefs.getInt("p1_wins", 0)) }
    var p2Wins by remember { mutableStateOf(prefs.getInt("p2_wins", 0)) }
    var draws by remember { mutableStateOf(prefs.getInt("draws", 0)) }

    val (winner, winLine) = checkConnectFourWin(board)

    fun executeDrop(col: Int, player: Int) {
        val r = getC4DropRow(col, board)
        if (r != -1 && winner == null) {
            val newBoard = board.toMutableList()
            newBoard[r * C4_COLS + col] = player
            board = newBoard
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val (newWin, _) = checkConnectFourWin(newBoard)
            if (newWin == null) {
                turn = if (player == 1) 2 else 1
            }
        }
    }

    LaunchedEffect(turn, isAiMode, winner, difficulty) {
        if (isAiMode && turn == 2 && winner == null) {
            isAiThinking = true
            val delayMs = if (difficulty == ConnectFourDifficulty.EASY) 450L else 600L
            delay(delayMs)
            val cpuCol = getConnectFourCpuColumn(board, difficulty)
            if (cpuCol != -1) {
                executeDrop(cpuCol, 2)
            }
            isAiThinking = false
        }
    }

    LaunchedEffect(winner) {
        when (winner) {
            1 -> {
                p1Wins++
                prefs.edit().putInt("p1_wins", p1Wins).apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            2 -> {
                p2Wins++
                prefs.edit().putInt("p2_wins", p2Wins).apply()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            0 -> {
                draws++
                prefs.edit().putInt("draws", draws).apply()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Connect Four", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isAiMode,
                        onClick = {
                            isAiMode = true
                            board = List(C4_ROWS * C4_COLS) { 0 }
                            turn = 1
                        },
                        label = { Text("VS AI") }
                    )
                    FilterChip(
                        selected = !isAiMode,
                        onClick = {
                            isAiMode = false
                            board = List(C4_ROWS * C4_COLS) { 0 }
                            turn = 1
                        },
                        label = { Text("2P") }
                    )
                }
            }

            AnimatedVisibility(visible = isAiMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Difficulty:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ConnectFourDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = {
                                if (difficulty != diff) {
                                    difficulty = diff
                                    board = List(C4_ROWS * C4_COLS) { 0 }
                                    turn = 1
                                }
                            },
                            label = { Text(diff.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            // Scoreboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("You (P1): $p1Wins", style = MaterialTheme.typography.bodySmall, color = BrandOrange, fontWeight = FontWeight.Bold)
                Text("Draws: $draws", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (isAiMode) "AI: $p2Wins" else "P2: $p2Wins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Status Banner
            val statusText = when {
                winner == 1 -> if (isAiMode) "🎉 You Won!" else "🎉 Player 1 Wins!"
                winner == 2 -> if (isAiMode) "🤖 AI Won!" else "🎉 Player 2 Wins!"
                winner == 0 -> "🤝 It's a Draw!"
                isAiThinking -> "🤖 AI is thinking..."
                turn == 1 -> if (isAiMode) "Your Turn (Orange)" else "Turn: Player 1 (Orange)"
                else -> if (isAiMode) "AI's Turn (Blue)..." else "Turn: Player 2 (Blue)"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (winner) {
                    1 -> BrandOrange
                    2 -> MaterialTheme.colorScheme.primary
                    0 -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // 7x6 Board
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0 until C4_ROWS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until C4_COLS) {
                            val idx = r * C4_COLS + c
                            val value = board[idx]
                            val isWinPiece = winLine?.contains(idx) == true

                            val chipColor = when (value) {
                                1 -> BrandOrange
                                2 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.background
                            }

                            val borderStroke = when {
                                isWinPiece -> BorderStroke(2.5.dp, Color.White)
                                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(chipColor)
                                    .border(borderStroke, CircleShape)
                                    .clickable(enabled = winner == null && (!isAiMode || (turn == 1 && !isAiThinking))) {
                                        executeDrop(c, turn)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWinPiece) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Buttons: Reset Board & Clear Scores
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        board = List(C4_ROWS * C4_COLS) { 0 }
                        turn = 1
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Board", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }

                if (p1Wins > 0 || p2Wins > 0 || draws > 0) {
                    OutlinedButton(
                        onClick = {
                            p1Wins = 0
                            p2Wins = 0
                            draws = 0
                            prefs.edit().clear().apply()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Reset Scores", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

// ── Native Game: Retro Snake ──────────────────────────────────────

@Composable
fun SnakeGameView() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("echowithin_games_snake", Context.MODE_PRIVATE) }

    val gridSize = 14
    var snake by remember { mutableStateOf(listOf(Pair(7, 7), Pair(7, 6), Pair(7, 5))) }
    var direction by remember { mutableStateOf(Pair(0, 1)) }
    var food by remember { mutableStateOf(Pair(4, 4)) }
    var isRunning by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(prefs.getInt("high_score", 0)) }

    var dragAccumulatorX by remember { mutableFloatStateOf(0f) }
    var dragAccumulatorY by remember { mutableFloatStateOf(0f) }

    fun spawnFood(currentSnake: List<Pair<Int, Int>>): Pair<Int, Int> {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val p = Pair(r, c)
                if (p !in currentSnake) empty.add(p)
            }
        }
        return if (empty.isNotEmpty()) empty.random() else Pair(0, 0)
    }

    LaunchedEffect(isRunning, isGameOver, score) {
        while (isRunning && !isGameOver) {
            // Speed accelerates slightly with score
            val currentDelay = (180L - (score / 20) * 8L).coerceAtLeast(85L)
            delay(currentDelay)
            val head = snake.first()
            val newHead = Pair(head.first + direction.first, head.second + direction.second)

            // Wall or self collision
            if (newHead.first !in 0 until gridSize || newHead.second !in 0 until gridSize || newHead in snake) {
                isGameOver = true
                isRunning = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (score > highScore) {
                    highScore = score
                    prefs.edit().putInt("high_score", score).apply()
                }
            } else {
                val newSnake = mutableListOf(newHead)
                if (newHead == food) {
                    newSnake.addAll(snake)
                    score += 10
                    food = spawnFood(newSnake)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (score > highScore) {
                        highScore = score
                        prefs.edit().putInt("high_score", score).apply()
                    }
                } else {
                    newSnake.addAll(snake.dropLast(1))
                }
                snake = newSnake
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Retro Snake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Score: $score", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandOrange)
                    Text("Best: $highScore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Grid with Drag/Swipe Gestures
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .pointerInput(isRunning, isGameOver) {
                        if (!isRunning || isGameOver) return@pointerInput
                        detectDragGestures(
                            onDragStart = {
                                dragAccumulatorX = 0f
                                dragAccumulatorY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulatorX += dragAmount.x
                                dragAccumulatorY += dragAmount.y
                            },
                            onDragEnd = {
                                val threshold = 18f
                                if (abs(dragAccumulatorX) > abs(dragAccumulatorY)) {
                                    if (abs(dragAccumulatorX) > threshold) {
                                        if (dragAccumulatorX > 0 && direction != Pair(0, -1)) {
                                            direction = Pair(0, 1) // Right
                                        } else if (dragAccumulatorX < 0 && direction != Pair(0, 1)) {
                                            direction = Pair(0, -1) // Left
                                        }
                                    }
                                } else {
                                    if (abs(dragAccumulatorY) > threshold) {
                                        if (dragAccumulatorY > 0 && direction != Pair(-1, 0)) {
                                            direction = Pair(1, 0) // Down
                                        } else if (dragAccumulatorY < 0 && direction != Pair(1, 0)) {
                                            direction = Pair(-1, 0) // Up
                                        }
                                    }
                                }
                            }
                        )
                    }
            ) {
                val cellSize = 240.dp / gridSize

                // Food
                Box(
                    modifier = Modifier
                        .offset(x = cellSize * food.second, y = cellSize * food.first)
                        .size(cellSize)
                        .padding(1.dp)
                        .clip(CircleShape)
                        .background(BrandOrange)
                )

                // Snake
                snake.forEachIndexed { index, segment ->
                    Box(
                        modifier = Modifier
                            .offset(x = cellSize * segment.second, y = cellSize * segment.first)
                            .size(cellSize)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }

                if (isGameOver) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Game Over!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Final Score: $score", color = BrandOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Text(
                text = "Swipe on board or tap D-pad to change direction",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (isGameOver) {
                            snake = listOf(Pair(7, 7), Pair(7, 6), Pair(7, 5))
                            direction = Pair(0, 1)
                            score = 0
                            isGameOver = false
                        }
                        isRunning = !isRunning
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text(if (isRunning) "Pause" else if (isGameOver) "Restart" else "Start")
                }
            }

            // D-Pad buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { if (direction != Pair(1, 0)) direction = Pair(-1, 0) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                }
                Row {
                    IconButton(onClick = { if (direction != Pair(0, 1)) direction = Pair(0, -1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left")
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    IconButton(onClick = { if (direction != Pair(0, -1)) direction = Pair(0, 1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right")
                    }
                }
                IconButton(onClick = { if (direction != Pair(-1, 0)) direction = Pair(1, 0) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                }
            }
        }
    }
}

// ── Components ──────────────────────────────────────────────────

@Composable
fun ArcadeGameLinkCard(
    title: String,
    description: String,
    url: String
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedButton(
                onClick = {
                    launchInAppGameTab(context, url)
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BrandOrange)
            ) {
                Text("Play", color = BrandOrange, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PartyRoomCard(
    title: String,
    badge: String,
    description: String,
    createType: String
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        launchInAppGameTab(context, "https://echowithin.xyz/games/create?type=$createType")
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("Host $title", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
