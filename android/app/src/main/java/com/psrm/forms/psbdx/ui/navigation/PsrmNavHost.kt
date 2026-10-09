package com.psrm.forms.psbdx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.psrm.forms.psbdx.PsrmApplication
import com.psrm.forms.psbdx.ui.formbuilder.FormBuilderScreen
import com.psrm.forms.psbdx.ui.formbuilder.FormBuilderViewModel
import com.psrm.forms.psbdx.ui.forms.FormsListScreen
import com.psrm.forms.psbdx.ui.forms.FormsListViewModel
import com.psrm.forms.psbdx.ui.login.LoginScreen
import com.psrm.forms.psbdx.ui.login.LoginViewModel
import com.psrm.forms.psbdx.ui.responses.ResponsesScreen
import com.psrm.forms.psbdx.ui.responses.ResponsesViewModel

private object Routes {
    const val LOGIN = "login"
    const val FORMS = "forms"
    const val BUILDER = "builder/{formId}"
    const val RESPONSES = "responses/{formId}"
    fun builder(formId: Long) = "builder/$formId"
    fun responses(formId: Long) = "responses/$formId"
}

@Composable
fun PsrmNavHost(app: PsrmApplication) {
    val navController: NavHostController = rememberNavController()
    val startDestination = if (app.credentialStore.isLoggedIn) Routes.FORMS else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val vm = viewModel { LoginViewModel(app.authRepository) }
            LoginScreen(
                viewModel = vm,
                onLoggedIn = {
                    navController.navigate(Routes.FORMS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FORMS) {
            val vm = viewModel { FormsListViewModel(app.formsRepository(), app.authRepository) }
            FormsListScreen(
                viewModel = vm,
                onOpenBuilder = { formId -> navController.navigate(Routes.builder(formId)) },
                onOpenResponses = { formId -> navController.navigate(Routes.responses(formId)) },
                onLogout = {
                    app.authRepository.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.FORMS) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.BUILDER,
            arguments = listOf(navArgument("formId") { type = NavType.LongType })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments!!.getLong("formId")
            val vm = viewModel { FormBuilderViewModel(app.formsRepository(), formId) }
            FormBuilderScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }

        composable(
            Routes.RESPONSES,
            arguments = listOf(navArgument("formId") { type = NavType.LongType })
        ) { backStackEntry ->
            val formId = backStackEntry.arguments!!.getLong("formId")
            val vm = viewModel { ResponsesViewModel(app.responsesRepository(), formId) }
            ResponsesScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
