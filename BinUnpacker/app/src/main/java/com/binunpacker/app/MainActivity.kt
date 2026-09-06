package com.binunpacker.app

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.binunpacker.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExtractedAdapter

    private var currentData: ByteArray? = null
    private var currentEntries: List<ExtractedEntry> = emptyList()
    private var currentBaseName: String = "bin_file"

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                loadAndScan(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ExtractedAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnSelectFile.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("*/*"))
        }

        binding.btnSaveAll.setOnClickListener {
            saveAllEntries()
        }
    }

    private fun loadAndScan(uri: Uri) {
        currentBaseName = queryDisplayName(uri)
        binding.tvFileInfo.text = "Loading: $currentBaseName"
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveAll.isEnabled = false
        adapter.updateItems(emptyList())
        binding.tvResultCount.text = ""

        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("File nahi padh paya")

                val result = withContext(Dispatchers.Default) {
                    BinExtractor.scan(bytes)
                }

                currentData = bytes
                currentEntries = result.entries

                binding.tvFileInfo.text =
                    "$currentBaseName  (${formatSize(result.totalSize)})"
                binding.tvResultCount.text =
                    "${result.entries.size} item(s) mile bin file ke andar"
                adapter.updateItems(result.entries)
                binding.btnSaveAll.isEnabled = result.entries.isNotEmpty()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.tvFileInfo.text = "File load nahi hui"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveAllEntries() {
        val data = currentData ?: return
        val entries = currentEntries
        if (entries.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSaveAll.isEnabled = false

        lifecycleScope.launch {
            try {
                val outputDir = File(
                    getExternalFilesDir(null),
                    "extracted_${currentBaseName.substringBeforeLast('.')}"
                )
                val count = withContext(Dispatchers.IO) {
                    BinExtractor.saveAll(data, entries, outputDir)
                }
                Toast.makeText(
                    this@MainActivity,
                    "$count file(s) save ho gayi:\n${outputDir.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Save error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSaveAll.isEnabled = true
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        var name = "bin_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
        return name
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.2f MB", mb)
    }
}
