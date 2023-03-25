package com.noteapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.noteapp.model.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseFirestore firestore;

    private RecyclerView rvNotes;
    private FloatingActionButton btnAdd;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        firestore = FirebaseFirestore.getInstance();
        myRef = database.getReference().child("Posts");

        rvNotes = findViewById(R.id.rv_notes);
        rvNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNote();
            }
        });
        //myRef = database.getReference("message");
        //login("admin@gmail.com", "123456");
        //createNewUser("nhthien.dut@gmail.com", "123456");
        //postDataToRealTimeDB();
        //readDataFromRealTimeDB();
        //postDatatoFireStore();
        //addPostData(new Post("Thien Nguyen", "Android with Firebase"));
        //addPostData(new Post("Test post", "Android"));
    }

    public void addNote() {

        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View mView = inflater.inflate(R.layout.add_note, null);
        mDialog.setView(mView);

        AlertDialog dialog = mDialog.create();
        dialog.setCancelable(true);

        Button btnSave = mView.findViewById(R.id.btn_save);
        EditText tvEditTitle = mView.findViewById(R.id.tv_edit_title);
        EditText tvEditContent = mView.findViewById(R.id.tv_edit_content);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = myRef.push().getKey();
                String title = tvEditTitle.getText().toString();
                String content = tvEditContent.getText().toString();

                myRef.child(id).setValue(new Post(id, title, content,getRandomColor())).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Add note Successfully !!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Add note Fail !!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        FirebaseRecyclerOptions<Post> options =
                new FirebaseRecyclerOptions.Builder<Post>()
                        .setQuery(myRef, Post.class)
                        .build();
        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<Post, PostHolder>(options) {
            @Override
            public PostHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.note_items, parent, false);
                return new PostHolder(view);
            }

            @Override
            protected void onBindViewHolder(PostHolder holder, int position, Post model) {
                holder.tvTitle.setText(model.getTitle());
                holder.tvContent.setText(model.getContent());
                holder.layoutNote.setBackgroundColor(Color.parseColor(model.getColor()));

                ImageView ivAction = holder.itemView.findViewById(R.id.iv_action);
                ivAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            popupMenu.setGravity(Gravity.END);
                        }
                        popupMenu.getMenu().add("Edit").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(@NonNull MenuItem item) {
                                UpdateNote(model.getId(), model.getTitle(), model.getContent());
                                return false;
                            }
                        });
                        popupMenu.getMenu().add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(@NonNull MenuItem item) {
                                String id = model.getId();
                                myRef.child(id).removeValue();
                                Toast.makeText(getApplicationContext(), "Delete Successfully !!", Toast.LENGTH_SHORT).show();
                                return true;
                            }
                        });
                        popupMenu.show();
                    }
                });
            }
        };

        rvNotes.setAdapter(adapter);
        adapter.startListening();

    }
    public void UpdateNote(String idNote, String Title, String Content){
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View mView = inflater.inflate(R.layout.add_note, null);
        mDialog.setView(mView);

        AlertDialog dialog = mDialog.create();
        dialog.setCancelable(true);

        Button btnSave = mView.findViewById(R.id.btn_save);
        EditText tvEditTitle = mView.findViewById(R.id.tv_edit_title);
        EditText tvEditContent = mView.findViewById(R.id.tv_edit_content);
        tvEditTitle.setText(Title);
        tvEditContent.setText(Content);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = idNote;
                String title = tvEditTitle.getText().toString();
                String content = tvEditContent.getText().toString();
                myRef.child(id).child("title").setValue(title);
                myRef.child(id).child("tontent").setValue(content);
                dialog.dismiss();
            }

        });
        dialog.show();
    }
    public static class PostHolder extends RecyclerView.ViewHolder {

        public TextView tvTitle;
        public TextView tvContent;
        public LinearLayout layoutNote;

        public PostHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tv_title);
            tvContent = view.findViewById(R.id.tv_content);
            layoutNote = view.findViewById(R.id.layout_note);
        }
    }

    private String getRandomColor() {
        ArrayList<String> colors = new ArrayList<>();
        colors.add("#efeff0");
        colors.add("#fdf156");
        colors.add("#ecffdf");
        colors.add("#ffedca");
        colors.add("#80c357");
        colors.add("#fff7e6");
        colors.add("#14b9d5");
        colors.add("#ffe7e1");
        Random random = new Random();
        return  colors.get(random.nextInt(colors.size()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_logout:
                mAuth.signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }









    // ============ LÀM QUEN VỚI FIREBASE =======================
//    private void login(String email, String password) {
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if(task.isSuccessful()) {
//                            Log.d("DEBUG", "Login Successful !!");
//                        } else {
//                            Log.d("DEBUG", "Login Fail !!");
//                        }
//                    }
//                });
//    }
//
//
//    private void createNewUser(String newUserEmail, String newUserPassword) {
//        mAuth.createUserWithEmailAndPassword(newUserEmail, newUserPassword)
//                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if(task.isSuccessful()) {
//                            Log.d("DEBUG", "Create user Successful !!");
//                        } else {
//                            Log.d("DEBUG", "Create user Fail !!");
//                        }
//                    }
//                });
//    }
//
//    private void resetPassword(String email) {
//        mAuth.sendPasswordResetEmail(email)
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if(task.isSuccessful()) {
//                            Log.d("DEBUG", "Reset password Successful !!");
//                        } else {
//                            Log.d("DEBUG", "Reset password Fail !!");
//                        }
//                    }
//                });
//    }
//
//    private void signOut() {
//        mAuth.signOut();
//    }
//    private void postDataToRealTimeDB(String data) {
//        // Write a message to the database
//        myRef.setValue(data)
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if(task.isSuccessful()) {
//                            Log.d("DEBUG", "Post data " + data +" Successful !!");
//                        } else {
//                            Log.d("DEBUG", "Post data " + data +"  Fail !!");
//                        }
//                    }
//                });
//    }
//
//    private void readDataFromRealTimeDB() {
//        myRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                String value = snapshot.getValue(String.class);
//                Log.d("DEBUG", "Value is: " + value);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Failed to read value
//                Log.w("DEBUG", "Failed to read value.", error.toException());
//            }
//        });
//    }
//
//    private void postDatatoFireStore() {
//        // Create a new user with a first and last name
//        Map<String, Object> user = new HashMap<>();
//        user.put("first", "Ada");
//        user.put("last", "Lovelace");
//        user.put("born", 1815);
//
//        // Add a new document with a generated ID
//        firestore.collection("users")
//                .add(user)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.d("DEBUG", "DocumentSnapshot added with ID: " + documentReference.getId());
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.w("DEBUG", "Error adding document", e);
//                    }
//                });
//    }
//
//    public void addPostData(Post data) {
//        DatabaseReference myRefRoot = database.getReference();
//        myRefRoot.child("posts").setValue(data)
//                .addOnCompleteListener(new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        if(task.isSuccessful()) {
//                            Log.d("DEBUG", "Post data Successful !!");
//                        } else {
//                            Log.d("DEBUG", "Post data Fail !!");
//                        }
//                    }
//                });
//    }
}