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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {
    private val _difficulty = MutableStateFlow("Normal")
    val difficulty: StateFlow<String> = _difficulty

    private val _rounds = MutableStateFlow(10)
    val rounds: StateFlow<Int> = _rounds

    private val _timePerRound = MutableStateFlow(10)
    val timePerRound: StateFlow<Int> = _timePerRound

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    fun setDifficulty(difficulty: String) {
        _difficulty.value = difficulty
    }

    fun setRounds(rounds: Int) {
        _rounds.value = rounds
    }

    fun setTimePerRound(timePerRound: Int) {
        _timePerRound.value = timePerRound
    }

    fun incrementScore() {
        _score.value++
    }

    fun resetScore() {
        _score.value = 0
    }
}


@Composable
fun App() {
    val navController = rememberNavController()
    val viewModel: GameViewModel = viewModel()

    NavHost(navController, startDestination = "menu") {
        composable("menu") { MenuScreen(navController, viewModel) }
        composable("Ajustes") {
            SettingsScreen(
                navController,
                viewModel.difficulty.collectAsState().value,
                { viewModel.setDifficulty(it) },
                viewModel.rounds.collectAsState().value,
                { viewModel.setRounds(it) },
                viewModel.timePerRound.collectAsState().value,
                { viewModel.setTimePerRound(it) }
            )
        }
        composable("juego") {
            GameScreen(
                navController,
                viewModel.difficulty.collectAsState().value,
                viewModel.rounds.collectAsState().value,
                viewModel.timePerRound.collectAsState().value,
                viewModel
            )
        }
        composable("resultado/{score}") { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toInt() ?: 0
            ResultScreen(navController, score)
        }
    }
}
@Composable
fun GameScreen(
    navController: NavController,
    difficulty: String,
    rounds: Int,
    timePerRound: Int,
    viewModel: GameViewModel
) {
    val questions = getQuestionsForDifficulty(difficulty).shuffled().take(rounds).map { (text, options, correctAnswer) ->
        Question(text, options, correctAnswer)
    }

    var currentQuestionIndex by remember { mutableStateOf(0) }
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
                navController.navigate("resultado/${viewModel.score.value}")
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
                "Rondas ${currentQuestionIndex + 1}/${questions.size}",
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

            // Opciones de respuesta
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                question!!.options.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { option ->
                            Button(
                                onClick = {
                                    if (!answered) {
                                        answered = true
                                        if (option == question!!.correctAnswer) {
                                            viewModel.incrementScore()
                                        }
                                        if (currentQuestionIndex < questions.size - 1) {
                                            currentQuestionIndex++
                                            question = questions.getOrNull(currentQuestionIndex)
                                        } else {
                                            navController.navigate("resultado/${viewModel.score.value}")
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
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Dificultad", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            listOf("Facil", "Normal", "Dificil").forEach { option ->
                Button(
                    onClick = { onDifficultyChange(option) },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(option)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Numero de Preguntas", style = MaterialTheme.typography.headlineMedium)
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

        Text("Tiempo por ronda: $timePerRound segundos", style = MaterialTheme.typography.headlineMedium)
        Slider(
            value = timePerRound.toFloat(),
            onValueChange = { onTimePerRoundChange(it.toInt()) },
            valueRange = 5f..30f,
            steps = 5
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Guardar Ajustes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Volver a menu")
        }
    }
}

@Composable
fun MenuScreen(navController: NavController, viewModel: GameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Menu", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            viewModel.resetScore()
            navController.navigate("juego")
        }) {
            Text("Empezar Juego")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("Ajustes") }) {
            Text("Ajustes")
        }
    }
}


@Composable
fun ResultScreen(navController: NavController, score: Int) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Your score: $score", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { navController.navigate("menu") }) { Text("Volver a menu") }
    }
}


data class Question(
    val text: String,
    val options: List<String>,
    val correctAnswer: String
)

fun getQuestionsForDifficulty(difficulty: String): List<Question> {
    return when (difficulty) {
        "Facil" -> listOf(
            Question("¿Cuál es la capital de Francia?", listOf("París", "Londres", "Berlín", "Roma"), "París"),
            Question("¿2 + 2?", listOf("3", "4", "5", "6"), "4"),
            Question("¿Cuántos continentes hay?", listOf("5", "6", "7", "8"), "7"),
            Question("¿Cuál es el océano más grande de la Tierra?", listOf("Atlántico", "Pacífico", "Índico", "Ártico"), "Pacífico"),
            Question("¿Cuál es el símbolo químico del agua?", listOf("H2O", "CO2", "NaCl", "O2"), "H2O"),
            Question("¿Cuál es el nombre del planeta más grande de nuestro sistema solar?", listOf("Marte", "Júpiter", "Venus", "Saturno"), "Júpiter"),
            Question("¿Cuántas patas tiene una araña?", listOf("6", "8", "10", "12"), "8"),
            Question("¿De qué color es una manzana?", listOf("Verde", "Roja", "Amarilla", "Morada"), "Roja"),
            Question("¿Cuál es el opuesto de negro?", listOf("Blanco", "Rojo", "Azul", "Verde"), "Blanco"),
            Question("¿Cuál es el nombre de la montaña más alta del mundo?", listOf("K2", "Kangchenjunga", "Monte Everest", "Lhotse"), "Monte Everest"),
            Question("¿Cuál es la capital de España?", listOf("Madrid", "Barcelona", "Valencia", "Sevilla"), "Madrid"),
            Question("¿Cuántos días hay en una semana?", listOf("5", "6", "7", "8"), "7"),
            Question("¿Cuál es el nombre del río que atraviesa Londres?", listOf("Támesis", "Sena", "Danubio", "Rin"), "Támesis"),
            Question("¿Cuál es el nombre de la moneda utilizada en los Estados Unidos?", listOf("Dólar", "Euro", "Libra", "Yen"), "Dólar"),
            Question("¿Cuál es el nombre del desierto más grande del mundo?", listOf("Sahara", "Gobi", "Arábigo", "Kalahari"), "Sahara")
        )
        "Normal" -> listOf(
            Question("¿Cuál es el planeta más grande?", listOf("Tierra", "Marte", "Júpiter", "Venus"), "Júpiter"),
            Question("¿Cuál es el número atómico del Oxígeno?", listOf("6", "7", "8", "9"), "8"),
            Question("¿Quién pintó la Mona Lisa?", listOf("Miguel Ángel", "Leonardo da Vinci", "Rafael", "Donatello"), "Leonardo da Vinci"),
            Question("¿Cuál es la capital de Italia?", listOf("Roma", "Milán", "Venecia", "Florencia"), "Roma"),
            Question("¿Cuál es la montaña más alta del mundo?", listOf("K2", "Kangchenjunga", "Monte Everest", "Lhotse"), "Monte Everest"),
            Question("¿Cuál es el nombre del segundo planeta más grande de nuestro sistema solar?", listOf("Marte", "Júpiter", "Venus", "Saturno"), "Saturno"),
            Question("¿Cuántos huesos hay en el cuerpo humano?", listOf("206", "213", "220", "227"), "206"),
            Question("¿Cuál es el símbolo químico del oro?", listOf("Go", "Gd", "Gl", "Au"), "Au"),
            Question("¿Cuál es el nombre del lago más grande del mundo?", listOf("Superior", "Victoria", "Huron", "Michigan"), "Superior"),
            Question("¿Cuál es el nombre de la moneda utilizada en Japón?", listOf("Yen", "Won", "Rupia", "Dólar"), "Yen"),
            Question("¿Cuál es el nombre del país más grande de Sudamérica?", listOf("Brasil", "Argentina", "Colombia", "Perú"), "Brasil"),
            Question("¿Cuántos dientes tiene un adulto humano?", listOf("28", "30", "32", "34"), "32"),
            Question("¿Cuál es el nombre de la estrella más cercana a la Tierra?", listOf("Sol", "Sirio", "Alfa Centauri", "Próxima Centauri"), "Sol"),
            Question("¿Cuál es el nombre del país más pequeño del mundo?", listOf("Mónaco", "Nauru", "Tuvalu", "Ciudad del Vaticano"), "Ciudad del Vaticano"),
            Question("¿Cuál es el nombre del río más largo del mundo?", listOf("Nilo", "Amazonas", "Yangtsé", "Misisipi"), "Nilo"))
        "Dificil" -> listOf(
            Question("¿Quién escribió '1984'?", listOf("Orwell", "Huxley", "Bradbury", "Asimov"), "Orwell"),
            Question("E=mc^2, ¿qué representa 'c'?", listOf("Carga", "Velocidad de la luz", "Corriente", "Constante"), "Velocidad de la luz"),
            Question("¿Cuál es la velocidad de la luz en el vacío?", listOf("299.792.458 m/s", "300.000.000 m/s", "250.000.000 m/s", "200.000.000 m/s"), "299.792.458 m/s"),
            Question("¿Cuál es el país más grande del mundo por área?", listOf("Rusia", "China", "EE.UU.", "Canadá"), "Rusia"),
            Question("¿Cuál es el nombre de la moneda utilizada en Japón?", listOf("Yen", "Won", "Rupia", "Dólar"), "Yen"),
            Question("¿Cuál es el nombre del primer humano en viajar al espacio?", listOf("Neil Armstrong", "Buzz Aldrin", "Yuri Gagarin", "John Glenn"), "Yuri Gagarin"),
            Question("¿Cuál es el nombre de la luna más grande de Saturno?", listOf("Titán", "Encélado", "Mimas", "Tetis"), "Titán"),
            Question("¿Cuál es el nombre del elemento más abundante en el universo?", listOf("Hidrógeno", "Helio", "Oxígeno", "Carbono"), "Hidrógeno"),
            Question("¿Cuál es el nombre del proceso por el cual las plantas convierten la energía lumínica en energía química?", listOf("Fotosíntesis", "Respiración", "Digestión", "Excreción"), "Fotosíntesis"),
            Question("¿Cuál es el nombre de la fuerza que evita que los objetos floten lejos de la Tierra?", listOf("Gravedad", "Magnetismo", "Fricción", "Flotabilidad"), "Gravedad"),
            Question("¿Cuál es el nombre de la teoría que describe el movimiento de los continentes de la Tierra?", listOf("Tectónica de placas", "Deriva continental", "Expansión del lecho marino", "Subducción"), "Tectónica de placas"),
            Question("¿Cuál es el nombre del fenómeno que causa las mareas en la Tierra?", listOf("Gravedad", "Magnetismo", "Fricción", "Flotabilidad"), "Gravedad"),
            Question("¿Cuál es el nombre de la galaxia que contiene nuestro sistema solar?", listOf("Vía Láctea", "Andrómeda", "Triángulo", "Sombrero"), "Vía Láctea"),
            Question("¿Cuál es el nombre del evento que se cree que inició el universo?", listOf("Big Bang", "Big Crunch", "Estado Estacionario", "Inflación"), "Big Bang"),
            Question("¿Cuál es el nombre del estudio de las estrellas y otros cuerpos celestes?", listOf("Astronomía", "Astrología", "Cosmología", "Física"), "Astronomía")

        )
        else -> emptyList()
    }
}