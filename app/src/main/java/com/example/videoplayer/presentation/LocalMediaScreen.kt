package com.example.videoplayer.presentation


import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMediaScreen(navController: NavHostController, viewModel: VideoPlayerViewModel) {

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        viewModel.loadLocalVideo()

    }

    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingDelteVideo by remember { mutableStateOf<LocalVideo?>(null) }

    val scope = rememberCoroutineScope()

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->


        if (result.resultCode == Activity.RESULT_OK) {

            pendingDelteVideo?.let { video ->

                scope.launch {

                    viewModel.removeVideoFromList(video)
                }

                Toast.makeText(context, "video deleted!", Toast.LENGTH_SHORT).show()
            }

        } else {

            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()

        }

        pendingDelteVideo = null


    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Local Media") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {

                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )
    }) { paddingValues ->

        when {

            viewModel.isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }


            }

            viewModel.localVideos.isEmpty() -> {


                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {

                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("No Video found", color = MaterialTheme.colorScheme.outline)

                }

            }

            else -> {


                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(viewModel.localVideos) { video ->

                        VideoRow(
                            video = video,
                            onClick = { navController.navigate("player/${video.id}") },
                            onDelete = { videoToDelete = video })
                    }


                }
            }


        }

    }

    videoToDelete?.let { video ->

        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video") },
            text = { Text("kya aap \"${video.title}\"  ko permanently delte karna chate hain ? \n\n yeh action undo nahi ho sakta!") },
            confirmButton = {
                TextButton(onClick = {
                    videoToDelete = null


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                        try {
                            val deleteRequest = MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(video.uri)
                            )

                            pendingDelteVideo = video

                            deleteLauncher.launch(

                                IntentSenderRequest.Builder(deleteRequest.intentSender).build()
                            )
                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Delete Failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                        scope.launch {

                            try {
                                var deleted = withContext(Dispatchers.IO) {

                                    context.contentResolver.delete(video.uri, null, null) > 0
                                }

                                if (deleted) {

                                    viewModel.removeVideoFromList(video)
                                    Toast.makeText(context, "video Deleted", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            } catch (securityException: SecurityException) {

                                val recoverableException =
                                    securityException as? RecoverableSecurityException

                                recoverableException?.let {


                                    pendingDelteVideo = video

                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(it.userAction.actionIntent.intentSender)
                                            .build()
                                    )

                                } ?: run {

                                    Toast.makeText(
                                        context,
                                        "cannot delete this video",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            }

                        }
                    } else {

                        scope.launch {

                            val success = viewModel.deleteVideo(video)

                            if (success) {

                                Toast.makeText(context, "video Deleted!", Toast.LENGTH_SHORT).show()
                            } else {

                                Toast.makeText(context, "Delete Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }){
                    Text("Delte", color =  MaterialTheme.colorScheme.error)

                }
            }, dismissButton = {

                TextButton(onClick = { videoToDelete = null}) {

                    Text("cancel")
                }
            })
    }

}