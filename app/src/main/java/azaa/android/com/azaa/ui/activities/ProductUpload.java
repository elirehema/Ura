package azaa.android.com.azaa.ui.activities;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import azaa.android.com.azaa.R;
import azaa.android.com.azaa.model.Product;
import azaa.android.com.azaa.util.Constants;
import butterknife.BindView;
import butterknife.ButterKnife;

import static azaa.android.com.azaa.network.Config.MY_PREFS_NAME;


public class ProductUpload extends Activity implements AdapterView.OnItemSelectedListener {
    public static final String TAG = "ProductUploadActivity";
    FirebaseFirestore db;
    StorageReference storageReference;
    @BindView(R.id.btnUpload)
    Button uploadImage;
    @BindView(R.id.btn_pick)
    Button pickImage;
    private final int SELECT_PHOTO = 1;
    @BindView(R.id.image)
    ImageView imageView;

    @BindView(R.id.text_image_title)
    TextInputEditText itemTitle;
    @BindView(R.id.text_price)
    TextInputEditText itemPrice;
    @BindView(R.id.text_description)
    TextInputEditText itemDesc;
    private Bitmap selectedImage;
    private String item, category;
    String itemName;
    SharedPreferences prefs;
    //private ProgressBar progressBar;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        setContentView(R.layout.activity_upload_image);
        ButterKnife.bind(this);
        prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();


        doUpload();

        imageView.setVisibility(View.GONE);
        uploadImage.setVisibility(View.GONE);
        itemTitle.setVisibility(View.GONE);

        uploadImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(itemTitle.getText())) {
                    itemTitle.setError("Title Required!");

                } else if (TextUtils.isEmpty(itemPrice.getText())) {
                    itemPrice.setError("Price Required!");
                } else if (TextUtils.isEmpty(itemDesc.getText())) {
                    itemDesc.setError("Description Required!");
                } else {
                    uploadProductToFireStore();
                }
            }
        });

        pickImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                doUpload();
            }
        });
    }


    public void doUpload() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK && imageReturnedIntent != null) {
                    try {
                        imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        imageView.setVisibility(View.VISIBLE);
                        uploadImage.setVisibility(View.VISIBLE);
                        imageView.setImageBitmap(selectedImage);
                        String text = "Change Image";
                        itemTitle.setVisibility(View.VISIBLE);
                        pickImage.setText(text);
                        setSpinnerAdapter();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    this.finish();
                }


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }


    private void uploadProductToFireStore() {
        Product product = new Product();
        product.setProductType(category);
        product.setProductEmail("email@example.com");
        product.setProductDescription(itemDesc.getText().toString().trim());
        product.setProductContacts("3291830192");
        product.setProductName(itemTitle.getText().toString());
        product.setProductLocation("Dar es Salaam");
        product.setProductPrice(itemPrice.getText().toString().trim());
        product.setProductId(itemName);
        if (imageUri != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading image ...");
            progressDialog.show();
            storageReference = FirebaseStorage.getInstance().getReference(Constants.DATABASE_PATH_UPLOADS).child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            UploadTask uploadTask = storageReference.putFile(imageUri);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                    product.setProductImage(storageReference.getName());
                    //adding to database
                    db.collection("products")
                            .add(product)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
                }
            });
        }


    }

    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private String imageToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imgByte = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgByte, Base64.DEFAULT);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
        item = parent.getItemAtPosition(i).toString();
        switch (item) {
            case "Clothes":
                category = "w";
                break;
            case "Shoes":
                category = "v";
                break;
            case "Jewellery":
                category = "j";
                break;
            case "MobilePhones":
                category = "p";
                break;
        }

        // Showing selected spinner item
        if (item.equals("Category")) {
            uploadImage.setEnabled(false);
            Toast.makeText(parent.getContext(), "Select Category", Toast.LENGTH_LONG).show();
            Log.d("IMAGE STRING", imageToString(selectedImage));
        } else {
            uploadImage.setEnabled(true);
            Toast.makeText(parent.getContext(), "You have Selected: " + item, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void setSpinnerAdapter() {
        Spinner spinner = (Spinner) findViewById(R.id.spinnerCategory);

        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements
        List<String> categories = new ArrayList<String>();
        categories.add("Category");
        categories.add("Clothes");
        categories.add("Shoes");
        categories.add("Jewellery");
        categories.add("MobilePhones");


        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
    }


}

