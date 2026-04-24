package com.roboblocker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.roboblocker.R
import com.roboblocker.databinding.FragmentDashboardBinding
import com.roboblocker.utils.startOfToday
import com.roboblocker.viewmodel.MainViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Service status
        vm.isServiceActive.observe(viewLifecycleOwner) { active ->
            binding.cardStatus.setCardBackgroundColor(
                resources.getColor(if (active) R.color.green_dark else R.color.red_dark, null)
            )
            binding.tvServiceStatus.text = if (active) "🛡️ Proteção Ativa" else "⚠️ Proteção Inativa"
            binding.tvServiceDesc.text = if (active)
                "Triagem de chamadas em execução"
            else
                "Toque para ativar a triagem de chamadas"
            binding.cardStatus.setOnClickListener {
                if (!active) activity?.recreate()
            }
        }

        // Stats
        vm.blockedCount.observe(viewLifecycleOwner) { count ->
            binding.tvBlocklistSize.text = count.toString()
        }
        vm.totalBlocked.observe(viewLifecycleOwner) { total ->
            binding.tvTotalBlocked.text = total.toString()
        }
        vm.blockedToday.observe(viewLifecycleOwner) { today ->
            binding.tvBlockedToday.text = today.toString()
        }

        // Recent logs preview
        vm.recentCallLogs.observe(viewLifecycleOwner) { logs ->
            val recent = logs.take(3)
            binding.tvLastBlocked.text = if (recent.isEmpty()) {
                "Nenhuma chamada bloqueada ainda"
            } else {
                recent.joinToString("\n") { log ->
                    val num = if (log.number.length > 4) log.number else "Oculto"
                    "• $num — ${log.reason.take(40)}"
                }
            }
        }

        // Quick toggles
        val prefs = com.roboblocker.App.instance.preferences
        binding.switchBlocking.isChecked = prefs.isBlockingEnabled
        binding.switchAi.isChecked = prefs.aiDetectionEnabled

        binding.switchBlocking.setOnCheckedChangeListener { _, checked ->
            prefs.isBlockingEnabled = checked
        }
        binding.switchAi.setOnCheckedChangeListener { _, checked ->
            prefs.aiDetectionEnabled = checked
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
