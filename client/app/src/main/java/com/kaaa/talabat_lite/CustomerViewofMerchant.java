package com.kaaa.talabat_lite;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CustomerViewofMerchant extends AppCompatActivity {

    private ItemAdapter itemAdapter;
    private List<ItemAdapter.itemData> itemList;
    private  ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    TextView merchantName, merchantRating, merchantKeywords;
    String merchantNameStr, merchantKeywordsStr;
    float rating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor();
        }
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_viewof_merchant); // Link to your XML layout
        RecyclerView recyclerView = findViewById(R.id.MerchantRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(this, itemList);
        recyclerView.setAdapter(itemAdapter);
        initUI();
        fetchMerchantInfo();
    }
    private void getMerchantInfo ()
    {
        try
        {
            Intent intent = getIntent();
            int merchId=intent.getIntExtra("merch_id",0);
            socketHelper.getInstance().connect();
            socketHelper.getInstance().sendInt(globals.GET_MERCHANT_HOME_INFO);
            Log.d("Test1.123","sending code " + globals.GET_MERCHANT_HOME_INFO);
            socketHelper.getInstance().sendInt(merchId);
            merchantNameStr = socketHelper.getInstance().recvString();
            merchantKeywordsStr = socketHelper.getInstance().recvString();
            rating = socketHelper.getInstance().recvFloat();
            socketHelper.getInstance().close();

        } catch (IOException e) {

            Log.e("MerchantHomeFragment", "Fatal!!",e);
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private void loadItemsFromServer() {
        new Thread(() -> {
            try {
                Intent intent = getIntent();
                int merchId=intent.getIntExtra("merch_id",0);
                socketHelper.getInstance().connect();
                socketHelper.getInstance().sendInt(globals.GET_ITEMS);
                socketHelper.getInstance().sendInt(merchId);
                int itemCount = socketHelper.getInstance().recvInt();
                Log.d("MerchantHome", "RECIEVED " + itemCount);

                List<ItemAdapter.itemData> tempItemList = new ArrayList<>();  // Temporary list to hold items

                for (int i = 0; i < itemCount; i++) {
                    int itemId = socketHelper.getInstance().recvInt();
                    String itemName = socketHelper.getInstance().recvString();
                    Log.d("Item Name", "RECIEVED " + itemName);
                    float itemPrice = socketHelper.getInstance().recvFloat();
                    String itemDescription = socketHelper.getInstance().recvString();
                    Bitmap img = socketHelper.getInstance().recvImg();

                    tempItemList.add(new ItemAdapter.itemData(itemId, itemName, itemDescription, itemPrice, img));
                }
                socketHelper.getInstance().close();
                // Update the main itemList and notify the adapter in the UI thread
                this.runOnUiThread(() -> {
                    itemList.clear();  // Clear existing data
                    itemList.addAll(tempItemList);  // Add the new items
                    itemAdapter.notifyDataSetChanged();  // Notify the adapter that data has changed
                });

                //fetchMerchantInfo();

            } catch (IOException e) {
                Log.e("MerchantHomeFragment", "Error loading items from server", e);
            }
        }).start(); // Run in a background thread
    }


    private void fetchMerchantInfo() {
        executor.execute(() -> {
            getMerchantInfo();
            mainHandler.post(() -> {
                loadItemsFromServer();
                updateUI();
                // Load items only after merchant info is fetched
            });
        });
    }
    protected void initUI()
    {
        merchantName = findViewById(R.id.merchantName);
        merchantKeywords = findViewById(R.id.merchantKeywords);
        merchantRating = findViewById(R.id.merchantRating);
    }
    @SuppressLint("DefaultLocale")
    private void updateUI()
    {
        merchantName.setText(merchantNameStr);
        merchantRating.setText(String.format("Rating: %.1f", rating));
        merchantKeywords.setText(merchantKeywordsStr);
    }
}