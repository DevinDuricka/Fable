package one.fable.fable.library

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.fable.fable.R
import one.fable.fable.database.entities.Audiobook
import one.fable.fable.database.entities.PROGRESS_FINISHED
import one.fable.fable.database.entities.PROGRESS_IN_PROGRESS
import one.fable.fable.database.entities.PROGRESS_NOT_STARTED
import one.fable.fable.exoplayer.ExoPlayerMasterObject

class BottomSheetChangeAudiobookProgress(val audiobook: Audiobook) : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inProgressString = getString(R.string.in_progress) // "In Progress"
        val finishedString = getString(R.string.finished) //"Finished"
        val notStartedString = getString(R.string.not_started) //"Not Started"

        val progressOptions = when (audiobook.progressState){
            PROGRESS_NOT_STARTED ->
                arrayOf(inProgressString, finishedString)
            PROGRESS_IN_PROGRESS ->
                arrayOf(notStartedString, finishedString)
            PROGRESS_FINISHED ->
                arrayOf(notStartedString, inProgressString)
            else ->
                arrayOf(notStartedString, inProgressString, finishedString)
        }

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(getString(R.string.change_audiobook_status))
        builder.setItems(progressOptions) { dialog, index ->
            var progress = index
            when (audiobook.progressState){
                PROGRESS_NOT_STARTED ->
                    progress++
                PROGRESS_IN_PROGRESS ->
                    if (index == 1) {
                        progress = PROGRESS_FINISHED
                    }
            }

            CoroutineScope(Dispatchers.IO).launch {
                audiobook.progressState = progress
                ExoPlayerMasterObject.updateAudiobook(audiobook)
            }
        }

        return builder.create()
        //return super.onCreateDialog(savedInstanceState)
    }
}