package com.example.echowithin.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.ui.theme.BrandOrange
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class GameCategory(val label: String) {
    ARCADE("Arcade & 1v1"),
    PARTY("Party & Lobbies")
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
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://echowithin.xyz/games/join?pin=$pinText"))
                                        context.startActivity(intent)
                                    }
                                })
                            )
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (pinText.length == 6) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://echowithin.xyz/games/join?pin=$pinText"))
                                        context.startActivity(intent)
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
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://echowithin.xyz/games/create"))
                                context.startActivity(intent)
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
                    }
                }
            }
        }
    }
}

// ── Native Game: Tic-Tac-Toe ──────────────────────────────────────

@Composable
fun TicTacToeView() {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var isXTurn by remember { mutableStateOf(true) }
    var isAiMode by remember { mutableStateOf(true) }
    var xWins by remember { mutableStateOf(0) }
    var oWins by remember { mutableStateOf(0) }
    var ties by remember { mutableStateOf(0) }

    fun checkWinner(b: List<String>): String? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            val (a, c1, c2) = line
            if (b[a].isNotEmpty() && b[a] == b[c1] && b[a] == b[c2]) {
                return b[a]
            }
        }
        if (b.all { it.isNotEmpty() }) return "Tie"
        return null
    }

    val winner = checkWinner(board)

    LaunchedEffect(isXTurn, isAiMode, winner) {
        if (isAiMode && !isXTurn && winner == null) {
            delay(350)
            val emptyIndices = board.indices.filter { board[it].isEmpty() }
            if (emptyIndices.isNotEmpty()) {
                val move = emptyIndices.random()
                val newBoard = board.toMutableList()
                newBoard[move] = "O"
                board = newBoard
                isXTurn = true
            }
        }
    }

    LaunchedEffect(winner) {
        if (winner == "X") xWins++
        if (winner == "O") oWins++
        if (winner == "Tie") ties++
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
                        onClick = { isAiMode = true; board = List(9) { "" }; isXTurn = true },
                        label = { Text("VS AI") }
                    )
                    FilterChip(
                        selected = !isAiMode,
                        onClick = { isAiMode = false; board = List(9) { "" }; isXTurn = true },
                        label = { Text("2P") }
                    )
                }
            }

            // Scoreboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("X (You): $xWins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandOrange)
                Text("Ties: $ties", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (isAiMode) "O (AI): $oWins" else "O: $oWins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }

            // Status Banner
            val statusText = when {
                winner == "X" -> "🎉 X Wins!"
                winner == "O" -> if (isAiMode) "🤖 AI Wins!" else "🎉 O Wins!"
                winner == "Tie" -> "🤝 It's a Tie!"
                isXTurn -> "Turn: X"
                else -> if (isAiMode) "Turn: AI..." else "Turn: O"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (winner != null) BrandOrange else MaterialTheme.colorScheme.onSurface
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
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable(enabled = cell.isEmpty() && winner == null && (!isAiMode || isXTurn)) {
                                        val newBoard = board.toMutableList()
                                        newBoard[idx] = if (isXTurn) "X" else "O"
                                        board = newBoard
                                        isXTurn = !isXTurn
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cell,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cell == "X") BrandOrange else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Reset Button
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
        }
    }
}

// ── Native Game: Connect Four ─────────────────────────────────────

@Composable
fun ConnectFourView() {
    val rows = 6
    val cols = 7
    var board by remember { mutableStateOf(List(rows * cols) { 0 }) } // 0=empty, 1=Player 1 (Orange), 2=Player 2 (Blue)
    var turn by remember { mutableStateOf(1) }
    var winner by remember { mutableStateOf<Int?>(null) }
    var p1Wins by remember { mutableStateOf(0) }
    var p2Wins by remember { mutableStateOf(0) }

    fun checkWin(b: List<Int>): Int? {
        fun get(r: Int, c: Int) = if (r in 0 until rows && c in 0 until cols) b[r * cols + c] else 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val p = get(r, c)
                if (p == 0) continue
                // Horizontal
                if (get(r, c + 1) == p && get(r, c + 2) == p && get(r, c + 3) == p) return p
                // Vertical
                if (get(r + 1, c) == p && get(r + 2, c) == p && get(r + 3, c) == p) return p
                // Diagonal /
                if (get(r + 1, c + 1) == p && get(r + 2, c + 2) == p && get(r + 3, c + 3) == p) return p
                // Diagonal \
                if (get(r - 1, c + 1) == p && get(r - 2, c + 2) == p && get(r - 3, c + 3) == p) return p
            }
        }
        if (b.all { it != 0 }) return 0 // Draw
        return null
    }

    fun dropInColumn(col: Int) {
        if (winner != null) return
        for (r in rows - 1 downTo 0) {
            val idx = r * cols + col
            if (board[idx] == 0) {
                val newBoard = board.toMutableList()
                newBoard[idx] = turn
                board = newBoard
                val win = checkWin(newBoard)
                if (win != null) {
                    winner = win
                    if (win == 1) p1Wins++
                    if (win == 2) p2Wins++
                } else {
                    turn = if (turn == 1) 2 else 1
                }
                break
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
                Text(
                    text = when (winner) {
                        1 -> "🎉 Player 1 (Orange) Wins!"
                        2 -> "🎉 Player 2 (Blue) Wins!"
                        0 -> "🤝 Draw!"
                        else -> "Turn: Player $turn"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (winner != null) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("P1: $p1Wins", style = MaterialTheme.typography.bodySmall, color = BrandOrange, fontWeight = FontWeight.Bold)
                Text("P2: $p2Wins", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            // 7x6 Board
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (r in 0 until rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until cols) {
                            val value = board[r * cols + c]
                            val chipColor = when (value) {
                                1 -> BrandOrange
                                2 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.background
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(chipColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                                    .clickable(enabled = winner == null) { dropInColumn(c) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    board = List(rows * cols) { 0 }
                    turn = 1
                    winner = null
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Board", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Native Game: Retro Snake ──────────────────────────────────────

@Composable
fun SnakeGameView() {
    val gridSize = 14
    var snake by remember { mutableStateOf(listOf(Pair(7, 7), Pair(7, 6), Pair(7, 5))) }
    var direction by remember { mutableStateOf(Pair(0, 1)) }
    var food by remember { mutableStateOf(Pair(4, 4)) }
    var isRunning by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }

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

    LaunchedEffect(isRunning, isGameOver) {
        while (isRunning && !isGameOver) {
            delay(180)
            val head = snake.first()
            val newHead = Pair(head.first + direction.first, head.second + direction.second)

            // Wall or self collision
            if (newHead.first !in 0 until gridSize || newHead.second !in 0 until gridSize || newHead in snake) {
                isGameOver = true
                isRunning = false
                if (score > highScore) highScore = score
            } else {
                val newSnake = mutableListOf(newHead)
                if (newHead == food) {
                    newSnake.addAll(snake)
                    score += 10
                    food = spawnFood(newSnake)
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

            // Grid
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
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
                        Text("Game Over!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

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
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
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
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://echowithin.xyz/games/create?type=$createType"))
                        context.startActivity(intent)
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
