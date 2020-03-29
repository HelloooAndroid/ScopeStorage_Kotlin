package com.seeker.scopestorage_kotlin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileDescriptor
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var imageUri: Uri? = null
    private var imageUrifilePath: String? = ""
    private val PICK_IMAGE = 100

    private lateinit var iv_img: ImageView
    private lateinit var click_me: Button
    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        result = findViewById(R.id.result);
        iv_img = findViewById(R.id.iv_img);
        click_me = findViewById(R.id.click_me);
        click_me.setOnClickListener {
            chooseImage()
        }

    }


    private fun setImageUri(): Uri {
        val folder = File("${getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
        folder.mkdirs()

        val file = File(folder, "Image_Tmp.jpg")
        if (file.exists())
            file.delete()
        file.createNewFile()
        imageUri = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + applicationContext.getString(R.string.file_provider_authorities),
            file
        )
        imageUrifilePath = file.absolutePath
        Log.d("IMAGE_PATH_1", "" + imageUri);
        Log.d("IMAGE_PATH_2", "" + imageUrifilePath);
        return imageUri!!
    }

    private fun setCropImageUri(): Uri {

        val folder = File("${getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
        folder.mkdirs()

        val file = File(folder, "Crop_Image_Tmp.jpg")
        if (file.exists())
            file.delete()
        file.createNewFile()

        /*val outputUri =
            FileProvider.getUriForFile(
                this,
                //applicationContext.getString(R.string.file_provider_authorities),
                "com.seeker.scopestorage_kotlin.fileProvider",
                file
            )*/

        Log.d("CROP_IMAGE_PATH", "" + file.absolutePath);
        return Uri.fromFile(file)!!
    }


    private fun chooseImage() {
        startActivityForResult(getPickImageIntent(), 100)
    }

    private fun getPickImageIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri())

        intentList = addIntentsToList(this, intentList, pickIntent)
        intentList = addIntentsToList(this, intentList, takePhotoIntent)

        if (intentList.size > 0) {
            chooserIntent = Intent.createChooser(
                intentList.removeAt(intentList.size - 1),
                getString(R.string.select_capture_image)
            )
            chooserIntent!!.putExtra(
                Intent.EXTRA_INITIAL_INTENTS,
                intentList.toTypedArray<Parcelable>()
            )
        }

        return chooserIntent
    }


    private fun addIntentsToList(
        context: Context,
        list: MutableList<Intent>,
        intent: Intent
    ): MutableList<Intent> {
        val resInfo: List<ResolveInfo> =
            context.getPackageManager().queryIntentActivities(intent, 0)
        for (resolveInfo in resInfo) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedIntent = Intent(intent)
            targetedIntent.setPackage(packageName)
            list.add(targetedIntent)
            Log.d("addIntentsToList", "Intent: " + intent.action + " package: " + packageName)
        }
        return list
    }


    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor: ParcelFileDescriptor? =
            contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    handleImageRequest(data)
                }
            }
            UCrop.REQUEST_CROP ->{
                if (resultCode == RESULT_OK ) {
                    val resultUri = UCrop.getOutput(data!!);

                    Glide.with(this@MainActivity)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .load(resultUri)
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                Log.d("", "" + e.toString())
                                result.text = e.toString()
                                return false
                            }

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                Log.d("", "" + resource.toString())
                                return false
                            }


                        })
                        .into(iv_img)
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    val cropError = UCrop.getError(data!!);
                    result.text=cropError?.message.toString()
                }
            }

        }
    }

    private fun handleImageRequest(data: Intent?) {
        val exceptionHandler = CoroutineExceptionHandler { _, t ->
            t.printStackTrace()
            //progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                t.localizedMessage ?: getString(R.string.filed_to_load_image),
                Toast.LENGTH_SHORT
            ).show()
            result.text = t.localizedMessage.toString()

        }

        GlobalScope.launch(Dispatchers.Main + exceptionHandler) {
            // progressBar.visibility = View.VISIBLE

            if (data?.data != null) {     //Photo from gallery
                imageUri = data.data
            }
            /*If photo from gallery then imageUri already have its uri.
             Since, same has been used as MediaStore.EXTRA_OUTPUT */
            if (imageUri.toString().isNotEmpty()) {
                val outputUri = setCropImageUri()
                UCrop.of(imageUri!!, outputUri!!)
                    .withAspectRatio(1F, 1F)
                    //.withMaxResultSize(maxWidth, maxHeight)
                    .start(this@MainActivity);
            }
            //progressBar.visibility = View.GONE

        }

    }


}
