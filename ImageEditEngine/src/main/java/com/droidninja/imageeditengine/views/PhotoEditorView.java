package com.droidninja.imageeditengine.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Dimension;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.droidninja.imageeditengine.R;
import com.droidninja.imageeditengine.utils.KeyboardHeightProvider;
import com.droidninja.imageeditengine.utils.MultiTouchListener;
import com.droidninja.imageeditengine.utils.Utility;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhotoEditorView extends FrameLayout implements ViewTouchListener,
    KeyboardHeightProvider.KeyboardHeightObserver {

  RelativeLayout container;
  RecyclerView recyclerView;
  CustomPaintView customPaintView;
  private String folderName;
  private ImageView imageView;
  private ImageView deleteView;
  private ViewTouchListener viewTouchListener;
  private View selectedView;
  private int selectViewIndex;
  private EditText inputTextET;
  private KeyboardHeightProvider keyboardHeightProvider;
  private float initialY;
  private View containerView;

  public PhotoEditorView(Context context) {
    super(context);
    init(context, null, 0);
  }

  public PhotoEditorView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs, 0);
  }

  public PhotoEditorView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs, defStyle);
  }

  private void init(Context context, AttributeSet attrs, int defStyle) {
    View view = inflate(getContext(), R.layout.photo_editor_view, null);
    container = view.findViewById(R.id.container);
    containerView = view.findViewById(R.id.container_view);
    recyclerView = view.findViewById(R.id.recyclerview);
    inputTextET = view.findViewById(R.id.add_text_et);
    customPaintView = view.findViewById(R.id.paint_view);
    inputTextET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          if (selectedView != null) {
            ((AutofitTextView) selectedView).setText(inputTextET.getText());
            Utility.hideSoftKeyboard((Activity) getContext());
          } else {
            createText(inputTextET.getText().toString());
            Utility.hideSoftKeyboard((Activity) getContext());
          }
          inputTextET.setVisibility(INVISIBLE);
        }
        return false;
      }
    });
    keyboardHeightProvider = new KeyboardHeightProvider((Activity) getContext());
    keyboardHeightProvider.setKeyboardHeightObserver(this);

    GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 5);
    recyclerView.setLayoutManager(gridLayoutManager);

//    StickerListAdapter stickerAdapter = new StickerListAdapter(new ArrayList<String>());
//    recyclerView.setAdapter(stickerAdapter);

    EmojiAdapter emojiAdapter = new EmojiAdapter(new ArrayList<String>());
    recyclerView.setAdapter(emojiAdapter);

    view.post(new Runnable() {
      @Override public void run() {
        keyboardHeightProvider.start();
      }
    });
    inputTextET.post(new Runnable() {
      @Override public void run() {
        initialY = inputTextET.getY();
      }
    });
    addView(view);
  }

  public void showPaintView() {
    recyclerView.setVisibility(GONE);
    inputTextET.setVisibility(GONE);
    Utility.hideSoftKeyboard((Activity) getContext());
    customPaintView.bringToFront();
  }

  public void setBounds(RectF bitmapRect) {
    customPaintView.setBounds(bitmapRect);
  }

  public void setColor(int selectedColor) {
    customPaintView.setColor(selectedColor);
  }

  public int getColor() {
    return customPaintView.getColor();
  }

  public Bitmap getPaintBit() {
    return customPaintView.getPaintBit();
  }

  public void hidePaintView() {
    containerView.bringToFront();
  }

  //text mode methods
  public void setImageView(ImageView imageView, ImageView deleteButton,
      ViewTouchListener viewTouchListener) {
    this.imageView = imageView;
    this.deleteView = deleteButton;
    this.viewTouchListener = viewTouchListener;
  }

  public void setTextColor(int selectedColor) {
    AutofitTextView autofitTextView = null;
    if(selectedView!=null){
      autofitTextView = (AutofitTextView) selectedView;
      autofitTextView.setTextColor(selectedColor);
    }
    else {
      View view = getViewChildAt(selectViewIndex);
      if(view!=null && view instanceof AutofitTextView) {
        autofitTextView = (AutofitTextView) view;
        autofitTextView.setTextColor(selectedColor);
      }
    }
    inputTextET.setTextColor(selectedColor);
  }

  @SuppressLint("ClickableViewAccessibility") public void addText() {
    inputTextET.setVisibility(VISIBLE);
    recyclerView.setVisibility(GONE);
    containerView.bringToFront();
    inputTextET.setText(null);
    Utility.showSoftKeyboard((Activity) getContext(), inputTextET);
  }

  public void hideTextMode(){
    Utility.hideSoftKeyboard((Activity) getContext());
    inputTextET.setVisibility(INVISIBLE);
  }

  @SuppressLint("ClickableViewAccessibility") @Override public void setOnTouchListener(OnTouchListener l) {
    super.setOnTouchListener(l);
    containerView.setOnTouchListener(l);
  }

  @SuppressLint("ClickableViewAccessibility") private void createText(String text){
    final AutofitTextView autofitTextView =
        (AutofitTextView) LayoutInflater.from(getContext()).inflate(R.layout.text_editor, null);
    autofitTextView.setId(container.getChildCount());
    autofitTextView.setText(text);
    autofitTextView.setTextColor(inputTextET.getCurrentTextColor());
    autofitTextView.setMaxTextSize(Dimension.SP,50);
    MultiTouchListener multiTouchListener =
        new MultiTouchListener(deleteView, container, this.imageView, true, this);
    multiTouchListener.setOnMultiTouchListener(new MultiTouchListener.OnMultiTouchListener() {

      @Override public void onRemoveViewListener(View removedView) {
        container.removeView(removedView);
        inputTextET.setText(null);
        inputTextET.setVisibility(INVISIBLE);
        selectedView = null;
      }
    });
    multiTouchListener.setOnGestureControl(new MultiTouchListener.OnGestureControl() {
      @Override public void onClick(View currentView) {
        if(currentView!=null) {
          selectedView = currentView;
          selectViewIndex = currentView.getId();
          inputTextET.setVisibility(VISIBLE);
          inputTextET.setText(((AutofitTextView) currentView).getText());
          inputTextET.setSelection(inputTextET.getText().length());
          Log.i("ViewNum", ":" + selectViewIndex + " " + ((AutofitTextView) currentView).getText());
        }

        Utility.showSoftKeyboard((Activity) getContext(), inputTextET);
      }

      @Override public void onLongClick() {

      }
    });
    autofitTextView.setOnTouchListener(multiTouchListener);

    RelativeLayout.LayoutParams params =
        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    container.addView(autofitTextView, params);

    selectViewIndex = container.getChildAt(container.getChildCount()-1).getId();
    selectedView = null;
  }

  @Override public void onStartViewChangeListener(View view) {
    Utility.hideSoftKeyboard((Activity) getContext());
    if (viewTouchListener != null) {
      viewTouchListener.onStartViewChangeListener(view);
    }
  }

  @Override public void onStopViewChangeListener(View view) {
    if (viewTouchListener != null) {
      viewTouchListener.onStopViewChangeListener(view);
    }
  }

  private View getViewChildAt(int index){
    if(index>container.getChildCount()-1){
      return null;
    }
    return container.getChildAt(index);
  }

  @Override public void onKeyboardHeightChanged(int height, int orientation) {
    if(height == 0){
      inputTextET.setY(initialY);
      inputTextET.requestLayout();
    }else {

      float newPosition = initialY - height;
      inputTextET.setY(newPosition);
      inputTextET.requestLayout();
    }
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    keyboardHeightProvider.close();
  }

  public void showStickers(String stickersFolder) {
    containerView.bringToFront();
    recyclerView.setVisibility(VISIBLE);
    inputTextET.setVisibility(GONE);
    Utility.hideSoftKeyboard((Activity) getContext());
    this.folderName = stickersFolder;
    StickerListAdapter stickerListAdapter = (StickerListAdapter) recyclerView.getAdapter();
    if(stickerListAdapter!=null){
      stickerListAdapter.setData(getStickersList(stickersFolder));
    }
  }
  public void showImoji() {
    containerView.bringToFront();
    recyclerView.setVisibility(VISIBLE);
    inputTextET.setVisibility(GONE);
    Utility.hideSoftKeyboard((Activity) getContext());
    EmojiAdapter emojiAdapter = (EmojiAdapter) recyclerView.getAdapter();
    if(emojiAdapter!=null){
      emojiAdapter.setData(getEmojis(getContext()));
    }
  }

  public void hideStickers(){
    recyclerView.setVisibility(GONE);
  }

  private List<String> getStickersList(String folderName){
    AssetManager assetManager = getContext().getAssets();
    try {
      String[] lists = assetManager.list(folderName);
      return  Arrays.asList(lists);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @SuppressLint("ClickableViewAccessibility") public void onItemClick(Bitmap bitmap) {
    recyclerView.setVisibility(GONE);
    ImageView stickerImageView =
        (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.sticker_view, null);
    stickerImageView.setImageBitmap(bitmap);
    stickerImageView.setId(container.getChildCount());
    MultiTouchListener multiTouchListener =
        new MultiTouchListener(deleteView, container, this.imageView, true, this);
    multiTouchListener.setOnMultiTouchListener(new MultiTouchListener.OnMultiTouchListener() {

      @Override public void onRemoveViewListener(View removedView) {
        container.removeView(removedView);
        selectedView = null;
      }
    });
    multiTouchListener.setOnGestureControl(new MultiTouchListener.OnGestureControl() {
      @Override public void onClick(View currentView) {
        if(currentView!=null) {
          selectedView = currentView;
          selectViewIndex = currentView.getId();
        }
      }

      @Override public void onLongClick() {

      }
    });
    stickerImageView.setOnTouchListener(multiTouchListener);

    RelativeLayout.LayoutParams params =
        new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
    container.addView(stickerImageView, params);
  }

  public void reset() {
    container.removeAllViews();
    customPaintView.reset();
    invalidate();
  }

  public void crop(Rect cropRect) {
    container.removeAllViews();
    customPaintView.reset();
    invalidate();
  }

  public class StickerListAdapter extends RecyclerView.Adapter<StickerListAdapter.ViewHolder> {

    private List<String> stickers;

    public StickerListAdapter(ArrayList<String> list) {
      stickers = list;
    }

    public void setData(List<String> stickersList) {
      this.stickers = stickersList;
      notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

      public ViewHolder(View v) {
        super(v);
      }
    }

    public void add(int position, String item) {
      stickers.add(position, item);
      notifyItemInserted(position);
    }

    public void remove(int position) {
      stickers.remove(position);
      notifyItemRemoved(position);
    }

    @Override
    public StickerListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      View v = inflater.inflate(R.layout.sticker_view, parent, false);
      // set the view's size, margins, paddings and layout parameters
      ViewHolder vh = new ViewHolder(v);
      return vh;
    }

    @Override public void onBindViewHolder(ViewHolder holder, int position) {
      final String path = stickers.get(position);
      holder.itemView.setOnClickListener(new OnClickListener() {
        @Override public void onClick(View view) {
          onItemClick(getImageFromAssetsFile(path));
        }
      });
      ((ImageView)holder.itemView).setImageBitmap(getImageFromAssetsFile(path));
    }

    @Override public int getItemCount() {
      return stickers.size();
    }

    private Bitmap getImageFromAssetsFile(String fileName) {
      Bitmap image = null;
      AssetManager am = getResources().getAssets();
      try {
        InputStream is = am.open(folderName +"/"+ fileName);
        image = BitmapFactory.decodeStream(is);
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return image;
    }
  }
  public class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.ViewHolder> {

    ArrayList<String> emojisList;
    public EmojiAdapter(ArrayList<String> list) {
      emojisList = list;
    }

    @Override
    public EmojiAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      Log.e("Adapter","onCreateViewHolder");
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_emoji, parent, false);
      return new EmojiAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final EmojiAdapter.ViewHolder holder, final int position) {
      Log.e("Adapter","onBindViewHolder");
      holder.txtEmoji.setText(emojisList.get(position));
      holder.txtEmoji.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          onItemClick(getEmojiAsBitmap(emojisList.get(holder.getLayoutPosition())));
        }
      });
    }
    public void setData(ArrayList<String> emojisList) {
      this.emojisList = emojisList;
      notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
      return emojisList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      TextView txtEmoji;

      ViewHolder(View itemView) {
        super(itemView);
        txtEmoji = itemView.findViewById(R.id.txtEmoji);
      }
    }
  }
  public Bitmap getEmojiAsBitmap(String emojiName) {
    final View emojiRootView = getLayout();
    final TextView emojiTextView = emojiRootView.findViewById(R.id.tvPhotoEditorText);
    emojiTextView.setTextSize(56);
    emojiTextView.setText(emojiName);

    emojiTextView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    Bitmap bitmap = Bitmap.createBitmap(emojiTextView.getMeasuredWidth(), emojiTextView.getMeasuredHeight(),
      Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    emojiTextView.layout(0, 0, emojiTextView.getMeasuredWidth(), emojiTextView.getMeasuredHeight());
    emojiTextView.draw(canvas);
    return bitmap;
  }
  /**
   * Provide the list of emoji in form of unicode string
   *
   * @param context context
   * @return list of emoji unicode
   */
  public static ArrayList<String> getEmojis(Context context) {
    ArrayList<String> convertedEmojiList = new ArrayList<>();
    String[] emojiList = context.getResources().getStringArray(R.array.photo_editor_emoji);
    for (String emojiUnicode : emojiList) {
      convertedEmojiList.add(convertEmoji(emojiUnicode));
    }
    return convertedEmojiList;
  }
  private static String convertEmoji(String emoji) {
    String returnedEmoji;
    try {
      int convertEmojiToInt = Integer.parseInt(emoji.substring(2), 16);
      returnedEmoji = new String(Character.toChars(convertEmojiToInt));
    } catch (NumberFormatException e) {
      returnedEmoji = "";
    }
    return returnedEmoji;
  }
  private View getLayout() {
        View rootView;
        LayoutInflater mLayoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = mLayoutInflater.inflate(R.layout.view_photo_editor_text, null);
        TextView txtTextEmoji = (TextView) rootView.findViewById(R.id.tvPhotoEditorText);
        if (txtTextEmoji != null) {
          txtTextEmoji.setGravity(Gravity.CENTER);
          txtTextEmoji.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    return rootView;
  }

}
