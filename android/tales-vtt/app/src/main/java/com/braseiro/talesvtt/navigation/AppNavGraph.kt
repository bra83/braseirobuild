package com.braseiro.talesvtt.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.braseiro.talesvtt.game.GameViewModel
import com.braseiro.talesvtt.game.MotorRouteHint
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.screens.BackpackScreen
import com.braseiro.talesvtt.screens.ClimaxScreen
import com.braseiro.talesvtt.screens.ConnectionsScreen
import com.braseiro.talesvtt.screens.EndingScreen
import com.braseiro.talesvtt.screens.EverydayScreen
import com.braseiro.talesvtt.screens.GroupScreen
import com.braseiro.talesvtt.screens.KidScreen
import com.braseiro.talesvtt.screens.LocationScreen
import com.braseiro.talesvtt.screens.LibraryScreen
import com.braseiro.talesvtt.screens.MapScreen
import com.braseiro.talesvtt.screens.MenuScreen
import com.braseiro.talesvtt.screens.MysteryScreen
import com.braseiro.talesvtt.screens.SceneScreen
import com.braseiro.talesvtt.screens.TroubleScreen
import androidx.compose.runtime.remember
import com.braseiro.talesvtt.audio.UniversalTtsV3Engine
import com.braseiro.talesvtt.screens.AdventuresScreen
import com.braseiro.talesvtt.screens.VoiceSettingsScreen
import androidx.compose.ui.platform.LocalContext
import com.braseiro.talesvtt.life.AdventureRepository
import com.braseiro.talesvtt.life.AdventurePackage

@Composable
fun AppNavGraph(
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  startDestination: String = AppDestination.MENU.route,
  gameViewModel: GameViewModel = viewModel()
) {
  val gameState by gameViewModel.state.collectAsStateWithLifecycle()
  val hasManualSave by gameViewModel.hasManualSave.collectAsStateWithLifecycle()
  val motorStatus by gameViewModel.motorStatus.collectAsStateWithLifecycle()
  val motorConfig by gameViewModel.motorConfig.collectAsStateWithLifecycle()
  val motorRoute by gameViewModel.motorRoute.collectAsStateWithLifecycle()
  val dataStatus by gameViewModel.dataStatus.collectAsStateWithLifecycle()
  val manualLibraryStatus by gameViewModel.manualLibraryStatus.collectAsStateWithLifecycle()
  val adventureStarted by gameViewModel.adventureStarted.collectAsStateWithLifecycle()

  val onNavigate: (AppDestination) -> Unit = { requested ->
    // Read the live StateFlow value here instead of the last composed snapshot. During
    // NEW ADVENTURE, started=true and route=SCENE can be emitted in the same frame.
    val startedNow = gameViewModel.adventureStarted.value
    val destination = if (!startedNow && requested in setOf(
        AppDestination.SCENE, AppDestination.GROUP, AppDestination.MYSTERY, AppDestination.MAP, AppDestination.BACKPACK
      )) AppDestination.MENU else requested
    navController.navigate(destination.route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = false }
      launchSingleTop = true
      restoreState = false
    }
  }

  val openInternal: (String) -> Unit = { route ->
    navController.navigate(route) { launchSingleTop = true }
  }

  // Validated Motor Barbara packets and deterministic local adventure creation can emit this navigation signal.
  LaunchedEffect(motorRoute) {
    when (motorRoute) {
      MotorRouteHint.TROUBLE -> openInternal(InternalRoutes.TROUBLE)
      MotorRouteHint.CLIMAX -> openInternal(InternalRoutes.CLIMAX)
      MotorRouteHint.ENDING -> openInternal(InternalRoutes.ENDING)
      MotorRouteHint.SCENE -> onNavigate(AppDestination.SCENE)
      MotorRouteHint.NONE -> Unit
    }
    if (motorRoute != MotorRouteHint.NONE) gameViewModel.consumeMotorRoute()
  }

  NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier,
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None }
  ) {
    composable(AppDestination.SCENE.route) {
      SceneScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        motorStatus = motorStatus,
        onDeclareAction = gameViewModel::submitPlayerIntent,
        onRetryOpening = gameViewModel::retryOpeningNarration
      )
    }
    composable(AppDestination.KID.route) {
      KidScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onExportKid = gameViewModel::exportKid,
        onImportKid = gameViewModel::importKid,
        onChangePortrait = gameViewModel::setKidPortrait,
        onRecordFloodScar = gameViewModel::recordFloodScar
      )
    }
    composable(AppDestination.GROUP.route) {
      GroupScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onResolveFrictionIntent = { gameViewModel.submitPlayerIntent("Quero reunir os Teens em um lugar seguro e próximo para tentar resolver a Friction do grupo e recuperar Condições, se a cena cumprir os requisitos.") }
      )
    }
    composable(AppDestination.MYSTERY.route) {
      MysteryScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onOpenConnections = { openInternal(InternalRoutes.CONNECTIONS) }
      )
    }
    composable(AppDestination.MAP.route) {
      MapScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onDeclareTravelIntent = gameViewModel::submitTravelIntent
      )
    }
    composable(AppDestination.BACKPACK.route) {
      BackpackScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onUseIconicItem = gameViewModel::submitIconicItemProposal,
        onUseItem = gameViewModel::submitMysteryItemProposal
      )
    }
    composable(AppDestination.MENU.route) {
      MenuScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        adventureStarted = adventureStarted,
        hasManualSave = hasManualSave,
        motorStatus = motorStatus,
        motorConfig = motorConfig,
        dataStatus = dataStatus,
        automaticBackupConfigured = gameViewModel.automaticBackupConfigured(),
        onSaveNow = gameViewModel::saveNow,
        onLoadSave = { gameViewModel.loadManualSave() },
        onSaveMotorConfig = gameViewModel::saveMotorConfig,
        onClearMotorLocalApiKey = gameViewModel::clearMotorLocalApiKey,
        onExportBackup = gameViewModel::exportCampaignBackup,
        onImportBackup = gameViewModel::importCampaignBackup,
        onSelectBackupDirectory = gameViewModel::configureAutomaticBackup,
        onExportKid = gameViewModel::exportKid,
        onImportKid = gameViewModel::importKid,
        onOpenLibrary = { openInternal(InternalRoutes.LIBRARY) },
        onStartRandomAdventure = gameViewModel::startRandomAdventure,
        onOpenAdventures = { openInternal(InternalRoutes.ADVENTURES) },
        onOpenVoiceSettings = { openInternal(InternalRoutes.VOICE_SETTINGS) },
        onCreateRandomKid = gameViewModel::createRandomKid,
        onSelectRulesProfile = gameViewModel::selectRulesProfile,
        onContinueAdventure = { onNavigate(AppDestination.SCENE) }
      )
    }
    composable(InternalRoutes.LIBRARY) {
      LibraryScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        status = manualLibraryStatus,
        onImportPdfs = gameViewModel::importManuals,
        onSearch = gameViewModel::searchManuals,
        onRemove = gameViewModel::removeManual
      )
    }
    composable(InternalRoutes.TROUBLE) {
      TroubleScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onRoll = gameViewModel::roll,
        onHelp = gameViewModel::submitHelpProposal,
        onLuck = gameViewModel::spendLuck,
        onPush = gameViewModel::push,
        onPride = gameViewModel::usePride,
        onShame = gameViewModel::useShame,
        onScar = gameViewModel::useScar,
        onInformationBonus = gameViewModel::applyInformationBonus,
        onContactItemBonus = gameViewModel::applyContactItemBonus,
        onLeadershipComfort = gameViewModel::applyLeadershipComfort,
        onPurchaseBonusEffect = gameViewModel::purchaseBonusEffect,
        onSubmitResult = gameViewModel::submitTestResultToMotor,
        onDeclareAction = gameViewModel::submitPlayerIntent
      )
    }
    composable(InternalRoutes.CONNECTIONS) {
      ConnectionsScreen(onNavigate = onNavigate, gameState = gameState)
    }
    composable(InternalRoutes.LOCAL) {
      LocationScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onDeclareAction = gameViewModel::submitPlayerIntent
      )
    }
    composable(InternalRoutes.EVERYDAY) {
      EverydayScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onDeclareAction = gameViewModel::submitPlayerIntent
      )
    }
    composable(InternalRoutes.CLIMAX) {
      ClimaxScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onStartKidScene = { if (gameViewModel.startExtendedKidScene()) openInternal(InternalRoutes.TROUBLE) },
        onAcceptCompromise = gameViewModel::acceptExtendedCompromise,
        onDeclineCompromise = gameViewModel::declineExtendedCompromise
      )
    }
    composable(InternalRoutes.ENDING) {
      EndingScreen(
        onNavigate = onNavigate,
        gameState = gameState,
        onSetXpAnswer = gameViewModel::setXpAnswer,
        onConfirmChanges = gameViewModel::confirmMysteryEnd,
        onImproveSkill = gameViewModel::improveSkill,
        onSetBirthdayPassed = gameViewModel::setBirthdayPassed,
        onApplyBirthdayAttribute = gameViewModel::applyBirthdayAttribute,
        onSaveCharacterChanges = gameViewModel::saveBetweenMysteryChanges,
        onConfirmCharacterChanges = gameViewModel::confirmBetweenMysteryReview,
        onPromoteMysteryItem = gameViewModel::promoteMysteryItemToIconic,
        onHealFloodScar = gameViewModel::healFloodScar,
        onSetFloodFriction = gameViewModel::setFloodFriction,
        onStartNextSession = {
          gameViewModel.startNextSession()
          if (gameViewModel.state.value.lastError == null) onNavigate(AppDestination.SCENE)
        }
      )
    }
    composable(InternalRoutes.ADVENTURES) {
      val context = LocalContext.current
      val repository = remember { AdventureRepository(context) }
      AdventuresScreen(
        repository = repository,
        currentProfile = gameState.rulesProfile.name,
        isAdventureActive = adventureStarted,
        onResumeAdventure = { onNavigate(AppDestination.SCENE) },
        onConcludeAdventure = {
          gameViewModel.concludeCurrentAdventure()
        },
        onCreateAiAdventure = {
          var createdPkg: AdventurePackage? = null
          var isAi = false
          gameViewModel.generateAdventureWithAi(
            profile = gameState.rulesProfile,
            onSuccess = { pkg, generatedByAi ->
              createdPkg = pkg
              isAi = generatedByAi
            },
            onError = {}
          )
          createdPkg?.let { Pair(it, isAi) } ?: Pair(
            com.braseiro.talesvtt.game.AdventureFactory.createProceduralAdventurePackage(gameState.rulesProfile.name),
            false
          )
        },
        onStartAdventure = { pkg ->
          gameViewModel.startAdventureFromPackage(pkg)
          onNavigate(AppDestination.SCENE)
        },
        onNavigateBack = { navController.popBackStack() }
      )
    }
    composable(InternalRoutes.VOICE_SETTINGS) {
      val context = LocalContext.current
      val ttsEngine = remember { UniversalTtsV3Engine(context) }
      VoiceSettingsScreen(
        ttsEngine = ttsEngine,
        onNavigateBack = { navController.popBackStack() }
      )
    }
  }
}