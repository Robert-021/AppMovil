package com.notesapp.ui.notes

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.notesapp.R
import com.notesapp.data.local.NotesDatabase
import com.notesapp.data.repository.NotesRepository
import com.notesapp.databinding.FragmentNotesBinding
import com.notesapp.model.Note
import com.notesapp.ui.adapter.NotesAdapter
import com.notesapp.viewmodel.NoteEvent
import com.notesapp.viewmodel.NotesViewModel
import com.notesapp.viewmodel.NotesViewModelFactory

/**
 * Fragment that displays the list of notes.
 */
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by activityViewModels {
        val database = NotesDatabase.getInstance(requireContext())
        val repository = NotesRepository(database.noteDao())
        NotesViewModelFactory(repository)
    }
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
        binding.recyclerNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotes.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val note = adapter.currentList[position]
                confirmDelete(note)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerNotes)
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
                adapter.notifyDataSetChanged()
            }
            .setOnCancelListener { adapter.notifyDataSetChanged() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
