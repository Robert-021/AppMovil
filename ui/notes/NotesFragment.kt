package com.notesapp.ui.notes

import android.os.Bundle
import android.view.*
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.notesapp.R
import com.notesapp.databinding.FragmentNotesBinding
import com.notesapp.model.Note
import com.notesapp.ui.adapter.NotesAdapter
import com.notesapp.viewmodel.NoteEvent
import com.notesapp.viewmodel.NotesViewModel

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    // activityViewModels() comparte el MISMO ViewModel con DetailFragment
    private val viewModel: NotesViewModel by activityViewModels()
    private lateinit var adapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        setupMenu()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClick = { note -> navigateToDetail(note.id) },
            onPinClick  = { note -> viewModel.togglePin(note) },
            onSwipeDelete = { note -> confirmDelete(note) }
        )
        binding.recyclerNotes.adapter = adapter
        adapter.attachSwipeToDelete(binding.recyclerNotes)
    }

    private fun setupFab() {
        binding.fabNewNote.setOnClickListener {
            viewModel.prepareNewNote()
            findNavController().navigate(R.id.action_notesFragment_to_detailFragment)
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_notes, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = "Buscar notas..."
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?) = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setSearchQuery(newText ?: "")
                        return true
                    }
                })
            }
            override fun onMenuItemSelected(menuItem: MenuItem) = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeViewModel() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
            // Muestra el estado vacío si no hay notas
            binding.tvEmptyState.isVisible = notes.isEmpty()
            binding.recyclerNotes.isVisible = notes.isNotEmpty()
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NoteEvent.DeleteSuccess ->
                    Snackbar.make(binding.root, "Nota eliminada", Snackbar.LENGTH_SHORT).show()
                is NoteEvent.Error ->
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                else -> {}
            }
            viewModel.clearEvent()
        }
    }

    private fun navigateToDetail(noteId: Long) {
        val action = NotesFragmentDirections.actionNotesFragmentToDetailFragment(noteId)
        findNavController().navigate(action)
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar nota")
            .setMessage("¿Estás seguro de que deseas eliminar \"${note.title}\"?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteNote(note) }
            .setNegativeButton("Cancelar") { _, _ ->
                // Restaura el item visualmente en el RecyclerView
                adapter.notifyDataSetChanged()
            }
            .setOnCancelListener { adapter.notifyDataSetChanged() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // Evita memory leaks
    }
}