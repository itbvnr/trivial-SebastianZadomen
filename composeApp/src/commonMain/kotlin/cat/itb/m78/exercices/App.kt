package cat.itb.m78.exercices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

sealed interface Screen {
    data object Menu : Screen
    data object Game : Screen
    data object Settings : Screen
    data class Result(val score: Int) : Screen
}

@Composable
fun App() {
    val navController = rememberNavController()
    var difficulty by remember { mutableStateOf("Normal") }
    var rounds by remember { mutableStateOf(10) }
    var timePerRound by remember { mutableStateOf(10) }

    NavHost(navController, startDestination = "menu") {
        composable("menu") { MenuScreen(navController) }
        composable("settings") {
            SettingsScreen(navController, difficulty, { difficulty = it }, rounds, { rounds = it }, timePerRound, { timePerRound = it })
        }
        composable("game") { GameScreen(navController, difficulty, rounds, timePerRound) }
        composable("result/{score}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toInt() ?: 0
            ResultScreen(navController, score)
        }
    }
}
@Composable
fun SettingsScreen(
    navController: NavController,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    rounds: Int,
    onRoundsChange: (Int) -> Unit,
    timePerRound: Int,
    onTimePerRoundChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        // Selector de Dificultad
        Text("Difficulty", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            listOf("Easy", "Normal", "Hard").forEach { option ->
                Button(
                    onClick = { onDifficultyChange(option) },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(option)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Number of Questions", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            listOf(5, 10, 15).forEach { option ->
                Button(
                    onClick = { onRoundsChange(option) },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(option.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Time per round: $timePerRound seconds", style = MaterialTheme.typography.headlineMedium)
        Slider(
            value = timePerRound.toFloat(),
            onValueChange = { onTimePerRoundChange(it.toInt()) },
            valueRange = 5f..30f,
            steps = 5
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Return to menu")
        }
    }
}
@Composable
fun GameScreen(
    navController: NavController,
    difficulty: String,
    rounds: Int,
    timePerRound: Int
) {
    val questions = getQuestionsForDifficulty(difficulty).shuffled().take(rounds).map { (text, options, correctAnswer) ->
        Question(text, options, correctAnswer)
    }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(timePerRound) }
    var question by remember { mutableStateOf(questions.getOrNull(currentQuestionIndex)) }
    var answered by remember { mutableStateOf(false) }

    LaunchedEffect(currentQuestionIndex) {
        timeLeft = timePerRound
        answered = false

        while (timeLeft > 0 && !answered) {
            delay(1000L)
            timeLeft--
        }

        if (!answered) {
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                question = questions.getOrNull(currentQuestionIndex)
            } else {
                navController.navigate("result/$score")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (question != null) {
            Text(
                "Round ${currentQuestionIndex + 1}/${questions.size}",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                question!!.text,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                question!!.options.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { option ->
                            Button(
                                onClick = {
                                    if (!answered) { // Only allow answer if not already answered
                                        answered = true
                                        if (option == question!!.correctAnswer) {
                                            score++
                                        }
                                        if (currentQuestionIndex < questions.size - 1) {
                                            currentQuestionIndex++
                                            question = questions.getOrNull(currentQuestionIndex) // Update question
                                        } else {
                                            navController.navigate("result/$score")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(8.dp)
                                    .height(80.dp)
                            ) {
                                Text(option, fontSize = 18.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = timeLeft.toFloat() / timePerRound.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
@Composable
fun MenuScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Menu", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { navController.navigate("game") }) {
            Text("Start Game")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("settings") }) {
            Text("Settings")
        }
    }
}
@Composable
fun ResultScreen(navController: NavController, score: Int) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Your score: $score", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { navController.navigate("menu") }) { Text("Return to menu") }
    }
}
data class Question(
    val text: String,
    val options: List<String>,
    val correctAnswer: String
)

fun getQuestionsForDifficulty(difficulty: String): List<Question> {
    return when (difficulty) {
        "Easy" -> listOf(
            Question("Capital of France?", listOf("Paris", "London", "Berlin", "Rome"), "Paris"),
            Question("2 + 2?", listOf("3", "4", "5", "6"), "4"),
            Question("How many continents are there?", listOf("5", "6", "7", "8"), "7"),
            Question("What is the largest ocean on Earth?", listOf("Atlantic", "Pacific", "Indian", "Arctic"), "Pacific"),
            Question("What is the chemical symbol for water?", listOf("H2O", "CO2", "NaCl", "O2"), "H2O"),
            Question("What is the name of the largest planet in our solar system?", listOf("Mars", "Jupiter", "Venus", "Saturn"), "Jupiter"),
            Question("How many legs does a spider have?", listOf("6", "8", "10", "12"), "8"),
            Question("What is the color of an apple?", listOf("Green", "Red", "Yellow", "Purple"), "Red"),
            Question("What is the opposite of black?", listOf("White", "Red", "Blue", "Green"), "White"),
            Question("What is the name of the tallest mountain in the world?", listOf("K2", "Kangchenjunga", "Mount Everest", "Lhotse"), "Mount Everest"),
            Question("What is the capital of Spain?", listOf("Madrid", "Barcelona", "Valencia", "Seville"), "Madrid"),
            Question("How many days are there in a week?", listOf("5", "6", "7", "8"), "7"),
            Question("What is the name of the river that flows through London?", listOf("Thames", "Seine", "Danube", "Rhine"), "Thames"),
            Question("What is the name of the currency used in the United States?", listOf("Dollar", "Euro", "Pound", "Yen"), "Dollar"),
            Question("What is the name of the largest desert in the world?", listOf("Sahara", "Gobi", "Arabian", "Kalahari"), "Sahara")
        )
        "Normal" -> listOf(
            Question("Largest planet?", listOf("Earth", "Mars", "Jupiter", "Venus"), "Jupiter"),
            Question("Atomic number of Oxygen?", listOf("6", "7", "8", "9"), "8"),
            Question("Who painted the Mona Lisa?", listOf("Michelangelo", "Leonardo da Vinci", "Raphael", "Donatello"), "Leonardo da Vinci"),
            Question("What is the capital of Italy?", listOf("Rome", "Milan", "Venice", "Florence"), "Rome"),
            Question("What is the highest mountain in the world?", listOf("K2", "Kangchenjunga", "Mount Everest", "Lhotse"), "Mount Everest"),
            Question("What is the name of the second largest planet in our solar system?", listOf("Mars", "Jupiter", "Venus", "Saturn"), "Saturn"),
            Question("How many bones are there in the human body?", listOf("206", "213", "220", "227"), "206"),
            Question("What is the chemical symbol for gold?", listOf("Go", "Gd", "Gl", "Au"), "Au"),
            Question("What is the name of the largest lake in the world?", listOf("Superior", "Victoria", "Huron", "Michigan"), "Superior"),
            Question("What is the name of the currency used in Japan?", listOf("Yen", "Won", "Rupee", "Dollar"), "Yen"),
            Question("What is the name of the largest country in South America?", listOf("Brazil", "Argentina", "Colombia", "Peru"), "Brazil"),
            Question("How many teeth does an adult human have?", listOf("28", "30", "32", "34"), "32"),
            Question("What is the name of the closest star to Earth?", listOf("Sun", "Sirius", "Alpha Centauri", "Proxima Centauri"), "Sun"),
            Question("What is the name of the smallest country in the world?", listOf("Monaco", "Nauru", "Tuvalu", "Vatican City"), "Vatican City"),
            Question("What is the name of the longest river in the world?", listOf("Nile", "Amazon", "Yangtze", "Mississippi"), "Nile")
        )
        "Hard" -> listOf(
            Question("Who wrote '1984'?", listOf("Orwell", "Huxley", "Bradbury", "Asimov"), "Orwell"),
            Question("E=mc^2, what does 'c' represent?", listOf("Charge", "Speed of light", "Current", "Constant"), "Speed of light"),
            Question("What is the speed of light in a vacuum?", listOf("299,792,458 m/s", "300,000,000 m/s", "250,000,000 m/s", "200,000,000 m/s"), "299,792,458 m/s"),
            Question("What is the largest country in the world by area?", listOf("Russia", "China", "USA", "Canada"), "Russia"),
            Question("What is the name of the currency used in Japan?", listOf("Yen", "Won", "Rupee", "Dollar"), "Yen"),
            Question("What is the name of the first human to travel to space?", listOf("Neil Armstrong", "Buzz Aldrin", "Yuri Gagarin", "John Glenn"), "Yuri Gagarin"),
            Question("What is the name of the largest moon of Saturn?", listOf("Titan", "Enceladus", "Mimas", "Tethys"), "Titan"),
            Question("What is the name of the most abundant element in the universe?", listOf("Hydrogen", "Helium", "Oxygen", "Carbon"), "Hydrogen"),
            Question("What is the name of the process by which plants convert light energy into chemical energy?", listOf("Photosynthesis", "Respiration", "Digestion", "Excretion"), "Photosynthesis"),
            Question("What is the name of the force that keeps objects from floating away from the Earth?", listOf("Gravity", "Magnetism", "Friction", "Buoyancy"), "Gravity"),
            Question("What is the name of the theory that describes the movement of the Earth's continents?", listOf("Plate tectonics", "Continental drift", "Seafloor spreading", "Subduction"), "Plate tectonics"),
            Question("What is the name of the phenomenon that causes the tides on Earth?", listOf("Gravity", "Magnetism", "Friction", "Buoyancy"), "Gravity"),
            Question("What is the name of the galaxy that contains our solar system?", listOf("Milky Way", "Andromeda", "Triangulum", "Sombrero"), "Milky Way"),
            Question("What is the name of the event that is believed to have started the universe?", listOf("Big Bang", "Big Crunch", "Steady State", "Inflation"), "Big Bang"),
            Question("What is the name of the study of the stars and other celestial bodies?", listOf("Astronomy", "Astrology", "Cosmology", "Physics"), "Astronomy")
        )
        else -> emptyList()
    }
}