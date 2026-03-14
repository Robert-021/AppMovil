package com.notesapp.ui.detail

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.notesapp.R
import com.notesapp.databinding.FragmentDetailBinding
import com.notesapp.viewmodel.NoteEvent
import com.notesapp.viewmodel.NotesViewModel

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by activityViewModels()

    // navArgs() lee el noteId que se pasó desde NotesFragment
    private val args: DetailFragmentArgs by navArgs()

    // Si noteId es -1 → es nota nueva; si tiene ID → es edición
    private val isNewNote get() = args.noteId == -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupMenu()
        observeViewModel()

        // Cargar nota existente o dejar campos vacíos para nueva nota
        if (!isNewNote) {
            viewModel.loadNote(args.noteId)
        } else {
            viewModel.prepareNewNote()
        }
    }

    private fun setupToolbar() {
        binding.toolbarDetail.title = if (isNewNote) "Nueva Nota" else "Editar Nota"
        binding.toolbarDetail.setNavigationOnClickListener {
            confirmExitIfNeeded()
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
                // El botón eliminar solo aparece cuando se edita una nota existente
                menu.findItem(R.id.action_delete)?.isVisible = !isNewNote
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> { saveNote(); true }
                    R.id.action_delete -> {
                        viewModel.currentNote.value?.let { confirmDelete(it) }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun observeViewModel() {
        // Rellena los campos SOLO si están vacíos
        // Esto evita sobreescribir el texto del usuario al rotar la pantalla
        viewModel.currentNote.observe(viewLifecycleOwner) { note ->
            note?.let {
                if (binding.etTitle.text.isNullOrEmpty() &&
                    binding.etContent.text.isNullOrEmpty()) {
                    binding.etTitle.setText(it.title)
                    binding.etContent.setText(it.content)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NoteEvent.SaveSuccess   -> findNavController().navigateUp()
                is NoteEvent.DeleteSuccess -> findNavController().navigateUp()
                is NoteEvent.Error -> {
                    Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                    if (event.message.contains("título")) {
                        binding.tilTitle.error = "El título es obligatorio"
                    }
                }
                else -> {}
            }
            viewModel.clearEvent()
        }
    }

    private fun saveNote() {
        // Limpia el error anterior antes de validar
        binding.tilTitle.error = null
        val title   = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()
        viewModel.saveNote(title, content)
    }

    private fun confirmDelete(note: com.notesapp.model.Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar nota")
            .setMessage("¿Eliminar \"${note.title}\"? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteNote(note) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Avisa al usuario si intenta salir sin guardar cambios
    private fun confirmExitIfNeeded() {
        val currentTitle   = binding.etTitle.text.toString()
        val currentContent = binding.etContent.text.toString()
        val originalNote   = viewModel.currentNote.value

        val hasChanges = if (originalNote == null) {
            currentTitle.isNotBlank() || currentContent.isNotBlank()
        } else {
            currentTitle != originalNote.title || currentContent != originalNote.content
        }

        if (hasChanges) {
            AlertDialog.Builder(requireContext())
                .setTitle("¿Salir sin guardar?")
                .setMessage("Tienes cambios sin guardar. ¿Deseas salir de todos modos?")
                .setPositiveButton("Salir")              { _, _ -> findNavController().navigateUp() }
                .setNegativeButton("Continuar editando", null)
                .show()
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}