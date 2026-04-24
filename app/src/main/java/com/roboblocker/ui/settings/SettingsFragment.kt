package com.roboblocker.ui.settings

import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.roboblocker.App
import com.roboblocker.databinding.FragmentSettingsBinding
import com.roboblocker.viewmodel.MainViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = App.instance.preferences

        // ─── Blocking settings ─────────────────────────────────────────────────
        binding.switchMainBlocking.isChecked    = prefs.isBlockingEnabled
        binding.switchBlockUnknown.isChecked    = prefs.blockUnknownNumbers
        binding.switchBlockInternational.isChecked = prefs.blockInternational
        binding.switchBlockPatterns.isChecked   = prefs.blockSpamPatterns
        binding.switchWhitelistContacts.isChecked = prefs.neverBlockContacts
        binding.switchNotifications.isChecked   = prefs.showBlockedNotification
        binding.switchFreqBlock.isChecked       = prefs.frequencyBlockEnabled

        binding.switchMainBlocking.setOnCheckedChangeListener    { _, v -> prefs.isBlockingEnabled = v }
        binding.switchBlockUnknown.setOnCheckedChangeListener    { _, v -> prefs.blockUnknownNumbers = v }
        binding.switchBlockInternational.setOnCheckedChangeListener { _, v -> prefs.blockInternational = v }
        binding.switchBlockPatterns.setOnCheckedChangeListener   { _, v -> prefs.blockSpamPatterns = v }
        binding.switchWhitelistContacts.setOnCheckedChangeListener { _, v -> prefs.neverBlockContacts = v }
        binding.switchNotifications.setOnCheckedChangeListener   { _, v -> prefs.showBlockedNotification = v }
        binding.switchFreqBlock.setOnCheckedChangeListener       { _, v -> prefs.frequencyBlockEnabled = v }

        // Frequency threshold
        binding.seekFreqThreshold.progress = prefs.frequencyThreshold - 1
        binding.tvFreqValue.text = "${prefs.frequencyThreshold} chamadas/hora"
        binding.seekFreqThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, user: Boolean) {
                val value = progress + 1
                prefs.frequencyThreshold = value
                binding.tvFreqValue.text = "$value chamadas/hora"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // ─── AI settings ───────────────────────────────────────────────────────
        binding.switchAiDetection.isChecked = prefs.aiDetectionEnabled
        binding.etApiKey.setText(prefs.aiApiKey)

        binding.switchAiDetection.setOnCheckedChangeListener { _, v -> prefs.aiDetectionEnabled = v }

        // AI sensitivity
        binding.rgSensitivity.check(
            when (prefs.aiSensitivity) {
                0    -> binding.rbLow.id
                2    -> binding.rbHigh.id
                else -> binding.rbMedium.id
            }
        )
        binding.rgSensitivity.setOnCheckedChangeListener { _, checkedId ->
            prefs.aiSensitivity = when (checkedId) {
                binding.rbLow.id  -> 0
                binding.rbHigh.id -> 2
                else              -> 1
            }
        }

        binding.btnSaveApiKey.setOnClickListener {
            prefs.aiApiKey = binding.etApiKey.text.toString().trim()
            com.roboblocker.utils.toast(requireContext(), "Chave de API salva!")
        }

        // ─── Schedule settings ─────────────────────────────────────────────────
        binding.switchSchedule.isChecked = prefs.scheduleEnabled
        binding.npStartHour.minValue = 0; binding.npStartHour.maxValue = 23
        binding.npEndHour.minValue   = 0; binding.npEndHour.maxValue   = 23
        binding.npStartHour.value = prefs.scheduleStartHour
        binding.npEndHour.value   = prefs.scheduleEndHour

        binding.switchSchedule.setOnCheckedChangeListener { _, v ->
            prefs.scheduleEnabled = v
            binding.layoutScheduleTime.visibility = if (v) View.VISIBLE else View.GONE
        }
        binding.layoutScheduleTime.visibility = if (prefs.scheduleEnabled) View.VISIBLE else View.GONE
        binding.npStartHour.setOnValueChangedListener { _, _, v -> prefs.scheduleStartHour = v }
        binding.npEndHour.setOnValueChangedListener   { _, _, v -> prefs.scheduleEndHour   = v }
    }

    private fun com.roboblocker.utils.toast(context: android.content.Context, msg: String) {
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
