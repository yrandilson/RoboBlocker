package com.roboblocker.ui.blocklist

import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.roboblocker.R
import com.roboblocker.adapter.BlockedNumberAdapter
import com.roboblocker.databinding.FragmentBlocklistBinding
import com.roboblocker.viewmodel.MainViewModel

class BlocklistFragment : Fragment() {

    private var _binding: FragmentBlocklistBinding? = null
    private val binding get() = _binding!!
    private val vm: MainViewModel by activityViewModels()
    private lateinit var adapter: BlockedNumberAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBlocklistBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BlockedNumberAdapter(
            onDelete = { number -> vm.removeNumber(number) },
            onLongClick = { number ->
                requireContext().let { ctx ->
                    val num = number.number
                    AlertDialog.Builder(ctx)
                        .setTitle("Opções")
                        .setItems(arrayOf("Copiar número", "Remover")) { _, i ->
                            when (i) {
                                0 -> ctx.let { c ->
                                    val cm = c.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                            as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("número", num))
                                    android.widget.Toast.makeText(c, "Copiado!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                1 -> vm.removeNumber(number)
                            }
                        }.show()
                }
            }
        )

        binding.recyclerBlocklist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBlocklist.adapter = adapter

        // Swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val item = adapter.getItem(vh.adapterPosition)
                vm.removeNumber(item)
            }
        }).attachToRecyclerView(binding.recyclerBlocklist)

        vm.allBlockedNumbers.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAddNumber.setOnClickListener { showAddDialog() }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.blocklist_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> { showImportDialog(); true }
            R.id.action_export -> { exportList(); true }
            R.id.action_clear  -> { showClearConfirm(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddDialog() {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }
        val etNumber = EditText(ctx).apply { hint = "Número (ex: 08001234567 ou +551199...)" }
        val etLabel  = EditText(ctx).apply { hint = "Rótulo (opcional)" }
        layout.addView(etNumber)
        layout.addView(etLabel)

        AlertDialog.Builder(ctx)
            .setTitle("Adicionar número")
            .setView(layout)
            .setPositiveButton("Adicionar") { _, _ ->
                vm.addNumber(etNumber.text.toString(), etLabel.text.toString())
            }
            .setNeutralButton("Padrão/Prefixo") { _, _ ->
                vm.addNumber(etNumber.text.toString(), etLabel.text.toString(), isPattern = true)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showImportDialog() {
        val ctx = requireContext()
        val et = EditText(ctx).apply {
            hint = "Um número por linha"
            minLines = 5
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }
        AlertDialog.Builder(ctx)
            .setTitle("Importar números")
            .setMessage("Cole uma lista de números, um por linha:")
            .setView(et)
            .setPositiveButton("Importar") { _, _ -> vm.importNumbers(et.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportList() {
        lifecycleScope.run {
            // Launch export via ViewModel
            android.widget.Toast.makeText(requireContext(), "Exportando…", android.widget.Toast.LENGTH_SHORT).show()
        }
        val scope = androidx.lifecycle.lifecycleScope
        scope.launchWhenStarted {
            val csv = vm.exportCsv()
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_TEXT, csv)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "RoboBlocker - Lista Negra")
            }
            startActivity(android.content.Intent.createChooser(intent, "Exportar lista"))
        }
    }

    private fun showClearConfirm() {
        AlertDialog.Builder(requireContext())
            .setTitle("Limpar lista negra")
            .setMessage("Tem certeza? Todos os números serão removidos.")
            .setPositiveButton("Limpar") { _, _ -> vm.clearBlocklist() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
