package com.notesapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notesapp.databinding.ItemNoteBinding
import com.notesapp.model.Note
import java.text.SimpleDateFormat
import java.util.Locale

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit,
    private val onSwipeDelete: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun attachSwipeToDelete(recyclerView: RecyclerView) {
        // Implementation for swipe to delete can be added here or in Fragment
        // For now, it's just a placeholder as it's called in Fragment
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.tvNoteTitle.text = note.title
            binding.tvNoteContent.text = note.content
            
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvNoteDate.text = sdf.format(note.updatedAt)

            binding.root.setOnClickListener { onNoteClick(note) }
            binding.ivPin.setOnClickListener { onPinClick(note) }
            
            // Visual feedback for pinned state
            binding.ivPin.alpha = if (note.isPinned) 1.0f else 0.3f
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
