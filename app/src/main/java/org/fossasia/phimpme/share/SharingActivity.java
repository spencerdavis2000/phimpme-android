package org.fossasia.phimpme.share;

import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.BOX;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.DROPBOX;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.FLICKR;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.OTHERS;
import static org.fossasia.phimpme.data.local.AccountDatabase.AccountName.TWITTER;
import static org.fossasia.phimpme.utilities.Constants.BOX_CLIENT_ID;
import static org.fossasia.phimpme.utilities.Constants.BOX_CLIENT_SECRET;
import static org.fossasia.phimpme.utilities.Constants.FAIL;
import static org.fossasia.phimpme.utilities.Constants.SUCCESS;
import static org.fossasia.phimpme.utilities.Utils.checkNetwork;
import static org.fossasia.phimpme.utilities.Utils.copyToClipBoard;
import static org.fossasia.phimpme.utilities.Utils.getBitmapFromPath;
import static org.fossasia.phimpme.utilities.Utils.getStringImage;
import static org.fossasia.phimpme.utilities.Utils.promptSpeechInput;
import static org.fossasia.phimpme.utilities.Utils.shareMsgOnIntent;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.listeners.ProgressListener;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.view.IconicsImageView;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.pinterest.android.pdk.PDKCallback;
import com.pinterest.android.pdk.PDKClient;
import com.pinterest.android.pdk.PDKException;
import com.pinterest.android.pdk.PDKResponse;
import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.User;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.fossasia.phimpme.R;
import org.fossasia.phimpme.base.PhimpmeProgressBarHandler;
import org.fossasia.phimpme.base.RecyclerItemClickListner;
import org.fossasia.phimpme.base.ThemedActivity;
import org.fossasia.phimpme.data.local.AccountDatabase;
import org.fossasia.phimpme.data.local.UploadHistoryRealmModel;
import org.fossasia.phimpme.editor.view.imagezoom.ImageViewTouch;
import org.fossasia.phimpme.gallery.activities.LFMainActivity;
import org.fossasia.phimpme.gallery.util.AlertDialogsHelper;
import org.fossasia.phimpme.gallery.util.ThemeHelper;
import org.fossasia.phimpme.share.flickr.FlickrHelper;
import org.fossasia.phimpme.share.tumblr.TumblrClient;
import org.fossasia.phimpme.share.twitter.HelperMethods;
import org.fossasia.phimpme.utilities.ActivitySwitchHelper;
import org.fossasia.phimpme.utilities.Constants;
import org.fossasia.phimpme.utilities.NotificationHandler;
import org.fossasia.phimpme.utilities.SnackBarHandler;
import org.fossasia.phimpme.utilities.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class which deals with Sharing images to multiple Account logged in by the user in the app. If
 * the account is not logged in from Account Manager, it shows a snackbar to login in Account
 * Manager first.
 *
 * <p>Click on the share account from bottom grid layout.
 *
 * <p>To Add new Account: 1. First add the entry in AccountDatabase AccountName enum
 *
 * <p>2. Add a icon in the format ic_<account_name>_black and, color in <account_name>_color in this
 * format. Because the color and icon assigning will be done in a loop to avoid the separate line
 * for each account.
 *
 * <p>3. Add the entry in Switch block for the click action on account. Create separate folder for
 * your share action, Don't code direcly inside the switch case.
 *
 * <p>4. Do add a documentation on the function.
 */
public class SharingActivity extends ThemedActivity
    implements View.OnClickListener,
        OnRemoteOperationListener,
        RecyclerItemClickListner.OnItemClickListener {

  public static final String EXTRA_OUTPUT = "extra_output";
  private static String LOG_TAG = SharingActivity.class.getCanonicalName();
  public String saveFilePath;
  ThemeHelper themeHelper;
  private OwnCloudClient mClient;
  private Handler mHandler;
  private ShareAdapter shareAdapter;

  @BindView(R.id.share_layout)
  View parent;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.share_image)
  ImageViewTouch shareImage;

  @BindView(R.id.edittext_share_caption)
  TextView text_caption;

  @BindView(R.id.share_account)
  RecyclerView shareAccountRecyclerView;

  @BindView(R.id.button_mic)
  IconicsImageView editFocus;

  @BindView(R.id.edit_text_caption_container)
  RelativeLayout captionLayout;

  private Realm realm = Realm.getDefaultInstance();
  private String caption;
  private PhimpmeProgressBarHandler phimpmeProgressBarHandler;
  private Context context;
  private BoxSession sessionBox;
  private ArrayList<AccountDatabase.AccountName> sharableAccountsList = new ArrayList<>();
  Bitmap finalBmp;
  Boolean isPostedOnTwitter = false, isPersonal = false;
  String boardID, imgurAuth = null, imgurString = null;
  private static final int REQ_SELECT_PHOTO = 1;
  private final int REQ_CODE_SPEECH_INPUT = 10;
  private static final int SHARE_WHATSAPP = 200;
  private static final int SHARE_SNAPCHAT = 200;

  public boolean uploadFailedBox = false;
  public String uploadName;
  private int positionShareOption;
  private boolean triedUploading = false;

  public static String getClientAuth() {
    return Constants.IMGUR_HEADER_CLIENt + " " + Constants.MY_IMGUR_CLIENT_ID;
  }

  @Override
  public void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    setContentView(R.layout.activity_share);
    caption = null;
    shareAdapter = new ShareAdapter();
    context = this;
    themeHelper = new ThemeHelper(this);
    mHandler = new Handler();
    sharableAccountsList = Utils.getSharableAccountsList();
    phimpmeProgressBarHandler = new PhimpmeProgressBarHandler(this);
    ActivitySwitchHelper.setContext(this);
    ButterKnife.bind(this);
    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    setupUI();
    initView();
    setUpRecyclerView();
    setStatusBarColor();
    configureBoxClient();
  }

  private void configureBoxClient() {
    BoxConfig.CLIENT_ID = BOX_CLIENT_ID;
    BoxConfig.CLIENT_SECRET = BOX_CLIENT_SECRET;
  }

  private void setupUI() {
    toolbar.setTitle(R.string.shareto);
    toolbar.setBackgroundColor(themeHelper.getPrimaryColor());
    toolbar.setNavigationIcon(getToolbarIcon(CommunityMaterial.Icon.cmd_arrow_left));
    setSupportActionBar(toolbar);
    parent.setBackgroundColor(themeHelper.getBackgroundColor());

    text_caption.setOnClickListener(this);
    editFocus.setOnClickListener(this);

    text_caption.getBackground().mutate().setColorFilter(getTextColor(), PorterDuff.Mode.SRC_ATOP);
    text_caption.setTextColor(getTextColor());
    text_caption.setHintTextColor(getSubTextColor());
    toolbar.setNavigationOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onBackPressed();
          }
        });
    editFocus.setColor(getIconColor());
  }

  private void initView() {
    saveFilePath = getIntent().getStringExtra(EXTRA_OUTPUT);
    Uri uri = Uri.fromFile(new File(saveFilePath));
    Glide.with(getApplicationContext())
        .load(uri)
        .diskCacheStrategy(DiskCacheStrategy.DATA)
        .into(shareImage);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.edittext_share_caption:
        openCaptionDialogBox();
        break;
      case R.id.button_mic:
        promptSpeechInput(
            SharingActivity.this,
            REQ_CODE_SPEECH_INPUT,
            parent,
            getString(R.string.speech_prompt_caption));
        break;
    }
  }

  @Override
  public void onItemClick(View childView, final int position) {
    positionShareOption = position;
    if (sharableAccountsList.get(position) == OTHERS) {
      triedUploading = true;
      shareToOthers();
      return;
    } else if (sharableAccountsList.get(position) != OTHERS) {
      if (!checkNetwork(this, parent)) return;

      final AlertDialog.Builder dialogBuilder =
          new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
      String msg =
          getString(R.string.are_you_sure) + " " + sharableAccountsList.get(position) + "?";
      AlertDialogsHelper.getTextDialog(
          SharingActivity.this, dialogBuilder, R.string.upload, 0, msg);
      dialogBuilder.setPositiveButton(
          R.string.ok_action,
          new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

              triedUploading = true;
              switch (sharableAccountsList.get(position)) {
                case TWITTER:
                  shareToTwitter();
                  break;

                case INSTAGRAM:
                  copyCaption();
                  break;

                case NEXTCLOUD:
                  shareToNextCloudAndOwnCloud(getString(R.string.nextcloud));
                  break;

                case PINTEREST:
                  shareToPinterest();
                  break;

                case MESSENGER:
                  shareToMessenger();
                  break;

                case FLICKR:
                  shareToFlickr();
                  break;

                case IMGUR:
                  shareToImgur();
                  break;

                case DROPBOX:
                  shareToDropBox();
                  break;

                  /* case GOOGLEDRIVE:
                  shareToGoogleDrive();
                  break;*/

                case OWNCLOUD:
                  shareToNextCloudAndOwnCloud(getString(R.string.owncloud));
                  break;

                case BOX:
                  shareToBox();
                  break;

                case TUMBLR:
                  shareToTumblr();
                  break;

                  /* case ONEDRIVE:
                  shareToOneDrive();
                  break;*/

                case OTHERS:
                  shareToOthers();
                  break;

                case WHATSAPP:
                  shareToWhatsapp();
                  break;

                  /* case GOOGLEPLUS:
                  shareToGoogle();
                  break;*/

                case SNAPCHAT:
                  shareToSnapchat();
                  break;

                default:
                  SnackBarHandler.create(parent, getString(R.string.feature_not_present)).show();
              }
            }
          });
      dialogBuilder.setNegativeButton(
          getString(R.string.cancel).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              // Do nothing
            }
          });
      AlertDialog alertDialog = dialogBuilder.create();
      alertDialog.show();
      AlertDialogsHelper.setButtonTextColor(
          new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
          getAccentColor(),
          alertDialog);
    }
  }

  private void shareToSnapchat() {
    Uri uri = Uri.fromFile(new File(saveFilePath));
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    share.putExtra(Intent.EXTRA_STREAM, uri);
    share.setType("image/*");
    share.putExtra(Intent.EXTRA_TEXT, caption);
    ComponentName intentComponent;
    intentComponent =
        new ComponentName("com.snapchat.android", "com.snapchat.android.LandingPageActivity");
    share.setComponent(intentComponent);
    startActivityForResult(
        Intent.createChooser(share, context.getString(R.string.snapchat)), SHARE_SNAPCHAT);
    triedUploading = true;
    sendResult(SUCCESS);
  }

  /**
   * Method to send the result of the share operation
   *
   * @param code - SUCCESS if user shares it FAIL otherwise.
   */
  private void sendResult(int code) {
    if (triedUploading) {
      triedUploading = false;
      Realm realm = Realm.getDefaultInstance();
      realm.beginTransaction();
      UploadHistoryRealmModel uploadHistory;
      uploadHistory = realm.createObject(UploadHistoryRealmModel.class);
      uploadHistory.setName(sharableAccountsList.get(positionShareOption).toString());
      uploadHistory.setPathname(saveFilePath);
      uploadHistory.setDatetime(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
      if (code == SUCCESS) {
        uploadHistory.setStatus(getString(R.string.upload_done));
        realm.commitTransaction();
        Intent result = new Intent();
        result.putExtra(Constants.SHARE_RESULT, code);
        setResult(RESULT_OK, result);
        finish();
      } else {
        uploadHistory.setStatus("FAIL");
        realm.commitTransaction();
      }
    } else {
      Intent result = new Intent();
      result.putExtra(Constants.SHARE_RESULT, code);
      setResult(RESULT_OK, result);
      finish();
    }
  }

  private void shareToTumblr() {
    new PostToTumblrAsync().execute();
  }

  @Override
  public void onItemLongPress(View childView, int position) {}

  private void shareToBox() {
    if (Utils.checkAlreadyExist(BOX)) {
      sessionBox = new BoxSession(this);
      new UploadToBox().execute();
    } else {
      SnackBarHandler.create(parent, getString(R.string.login_box)).show();
    }
  }

  private class UploadToBox extends AsyncTask<Void, Integer, Void> {
    private FileInputStream inputStream;
    private File file;
    private BoxApiFile mFileApi;
    private Boolean success;
    private int fileLength;

    @Override
    protected void onPreExecute() {
      sessionBox.authenticate();
      NotificationHandler.make(
          R.string.box, R.string.upload_progress, R.drawable.ic_cloud_upload_black_24dp);
      mFileApi = new BoxApiFile(sessionBox);
      file = new File(saveFilePath);
      fileLength = (int) file.length();
      NotificationHandler.actionProgress(0, fileLength, 0, R.string.progress);
      try {
        inputStream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      try {
        String destinationFolderId = "0";
        if (!uploadFailedBox) uploadName = file.getName();
        BoxRequestsFile.UploadFile request =
            mFileApi.getUploadRequest(inputStream, uploadName, destinationFolderId);
        final BoxFile uploadFileInfo =
            request
                .setProgressListener(
                    new ProgressListener() {
                      @Override
                      public void onProgressChanged(long l, long l1) {
                        int percent = ((int) l * 100) / fileLength;
                        NotificationHandler.actionProgress(
                            (int) l, fileLength, percent, R.string.upload_progress);
                      }
                    })
                .send();
        Log.d(LOG_TAG, uploadFileInfo.toString());
        success = true;
      } catch (BoxException e) {
        success = false;
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      if (success) {
        NotificationHandler.actionPassed(R.string.upload_complete);
        SnackBarHandler.create(parent, getString(R.string.uploaded_box)).show();
        sendResult(Constants.SUCCESS);
      } else {
        NotificationHandler.actionFailed();
        Snackbar.make(parent, getString(R.string.upload_failed_retry_box), Snackbar.LENGTH_LONG)
            .setAction(
                getString(R.string.retry_upload),
                new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                    uploadFailedBox = true;
                    renameUploadName(file.getName());
                  }
                })
            .show();
        sendResult(FAIL);
      }
    }
  }

  private void renameUploadName(String fileName) {
    final AlertDialog.Builder dialogBuilder =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    final EditText editTextNewName = new EditText(getApplicationContext());
    editTextNewName.setText(fileName);
    editTextNewName.setSelection(fileName.length());
    AlertDialogsHelper.getInsertTextDialog(
        SharingActivity.this, dialogBuilder, editTextNewName, R.string.Rename, null);

    dialogBuilder.setPositiveButton(
        getString(R.string.retry_upload).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            uploadName = editTextNewName.getText().toString();
            new UploadToBox().execute();
          }
        });
    AlertDialog alertDialog = dialogBuilder.create();
    alertDialog.show();
    AlertDialogsHelper.setButtonTextColor(
        new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
        getAccentColor(),
        alertDialog);
  }

  private void shareToFlickr() {
    if (Utils.checkAlreadyExist(FLICKR)) {
      SnackBarHandler.create(parent, getString(R.string.uploading)).show();
      InputStream is = null;
      File file = new File(saveFilePath);
      try {
        is = getContentResolver().openInputStream(Uri.fromFile(file));
      } catch (Exception e) {
        e.printStackTrace();
        sendResult(FAIL);
      }
      if (is != null) {
        FlickrHelper f = FlickrHelper.getInstance();
        f.setInputStream(is);
        f.setFilename(file.getName());

        if (caption != null && !caption.isEmpty()) f.setDescription(caption);
        f.uploadImage();
        sendResult(SUCCESS);
      }
    }
  }

  private void shareToDropBox() {
    RealmQuery<AccountDatabase> query = realm.where(AccountDatabase.class);
    // Checking if string equals to is exist or not
    query.equalTo("name", DROPBOX.toString());
    AccountDatabase result = query.findFirst();
    if (result != null) {
      new UploadToDropbox(result.getToken()).execute(saveFilePath);
    } else {
      SnackBarHandler.create(parent, getString(R.string.login_dropbox_account)).show();
    }
  }

  private class UploadToDropbox extends AsyncTask<String, Integer, Void> {
    Boolean success;
    DbxRequestConfig config;
    DbxClientV2 client;

    UploadToDropbox(String accessToken) {
      config = DbxRequestConfig.newBuilder("phimpme").build();
      client = new DbxClientV2(config, accessToken);
    }

    @Override
    protected void onPreExecute() {
      NotificationHandler.make(
          R.string.dropbox_share, R.string.upload_progress, R.drawable.ic_cloud_upload_black_24dp);
    }

    @Override
    protected Void doInBackground(String... args) {
      File file = new File(args[0]);
      try (InputStream inputStream = new FileInputStream(file)) {
        client
            .files()
            .uploadBuilder("/phimpme/" + file.getName())
            .withMode(WriteMode.OVERWRITE)
            .uploadAndFinish(inputStream);
        success = true;
      } catch (DbxException | IOException e) {
        success = false;
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      if (success) {
        NotificationHandler.actionPassed(R.string.upload_complete);
        SnackBarHandler.create(parent, getString(R.string.uploaded_dropbox)).show();
      } else {
        NotificationHandler.actionFailed();
        SnackBarHandler.create(parent, getString(R.string.upload_failed)).show();
      }
    }
  }

  @Override
  public void onBackPressed() {
    if (caption == null) {
      sendResult(FAIL);
    } else {
      final AlertDialog.Builder discardChangesDialogBuilder =
          new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
      AlertDialogsHelper.getTextDialog(
          SharingActivity.this,
          discardChangesDialogBuilder,
          R.string.discard_changes_header,
          R.string.discard_changes_message,
          null);
      discardChangesDialogBuilder.setPositiveButton(
          getString(R.string.ok).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              sendResult(FAIL);
            }
          });
      discardChangesDialogBuilder.setNegativeButton(
          getString(R.string.cancel).toUpperCase(),
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              if (dialog != null) dialog.dismiss();
            }
          });
      AlertDialog alertDialog = discardChangesDialogBuilder.create();
      alertDialog.show();
      AlertDialogsHelper.setButtonTextColor(
          new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
          getAccentColor(),
          alertDialog);
    }
  }

  private void openCaptionDialogBox() {
    final AlertDialog.Builder captionDialogBuilder =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    final EditText captionEditText = new EditText(this);
    AlertDialogsHelper.getInsertTextDialog(
        SharingActivity.this, captionDialogBuilder, captionEditText, R.string.caption_head, null);
    captionDialogBuilder.setNegativeButton(getString(R.string.cancel).toUpperCase(), null);
    captionEditText.setHint(R.string.description_hint);
    captionEditText.setHintTextColor(ContextCompat.getColor(this, R.color.grey));
    captionEditText.setSelectAllOnFocus(true);
    if (getBaseTheme() == ThemeHelper.DARK_THEME || getBaseTheme() == ThemeHelper.AMOLED_THEME) {
      captionEditText.setHighlightColor(
          ContextCompat.getColor(getApplicationContext(), R.color.accent_grey));
    } else
      captionEditText.setHighlightColor(
          ContextCompat.getColor(getApplicationContext(), R.color.cardview_shadow_start_color));
    captionEditText.selectAll();
    captionEditText.setSingleLine(false);
    captionDialogBuilder.setPositiveButton(
        getString(R.string.add_action).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // This should br empty it will be overwrite later
            // to avoid dismiss of the dialog on wrong password
          }
        });
    captionDialogBuilder.setNeutralButton(
        getString(R.string.delete).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // This shoud be overriden later
          }
        });
    final AlertDialog passwordDialog = captionDialogBuilder.create();
    passwordDialog
        .getWindow()
        .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    passwordDialog.show();

    if (caption != null) {
      captionEditText.setText(caption);
      captionEditText.setSelection(caption.length());
      passwordDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
      passwordDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(getAccentColor());
    } else {
      passwordDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
      passwordDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(getColor(R.color.grey));
    }

    AlertDialogsHelper.setButtonTextColor(
        new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
        getAccentColor(),
        passwordDialog);
    passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    AlertDialogsHelper.setButtonTextColor(
        new int[] {DialogInterface.BUTTON_POSITIVE}, getColor(R.color.grey), passwordDialog);
    captionEditText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // empty method body

          }

          @Override
          public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            // empty method body

          }

          @Override
          public void afterTextChanged(Editable editable) {
            if (TextUtils.isEmpty(editable)) {
              // Disable ok button and Delete button
              passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
              AlertDialogsHelper.setButtonTextColor(
                  new int[] {DialogInterface.BUTTON_POSITIVE},
                  getColor(R.color.grey),
                  passwordDialog);
            } else {
              // Something into edit text. Enable the button.
              passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
              passwordDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
              AlertDialogsHelper.setButtonTextColor(
                  new int[] {DialogInterface.BUTTON_POSITIVE}, getAccentColor(), passwordDialog);
            }
          }
        });
    passwordDialog
        .getButton(DialogInterface.BUTTON_NEUTRAL)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                caption = null;
                text_caption.setText(caption);
                captionEditText.setText(text_caption.toString());
                passwordDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
                passwordDialog.dismiss();
              }
            });
    passwordDialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                String captionText = captionEditText.getText().toString();
                if (!captionText.isEmpty()) {
                  caption = captionText;
                  text_caption.setText(caption);
                  captionEditText.setSelection(caption.length());
                } else {
                  caption = null;
                  text_caption.setText(caption);
                }
                passwordDialog.dismiss();
              }
            });
  }

  private void shareToPinterest() {
    final AlertDialog.Builder dialogBuilder =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    final EditText captionEditText = new EditText(getApplicationContext());

    String link = context.getString(R.string.Pinterest_link);
    AlertDialogsHelper.getInsertTextDialog(
        SharingActivity.this, dialogBuilder, captionEditText, R.string.Pinterest_link, link);
    dialogBuilder.setNegativeButton(getString(R.string.cancel).toUpperCase(), null);
    dialogBuilder.setPositiveButton(
        getString(R.string.post_action).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // This should br empty it will be overwrite later
            // to avoid dismiss of the dialog on wrong password
          }
        });

    final AlertDialog passwordDialog = dialogBuilder.create();
    passwordDialog.show();
    AlertDialogsHelper.setButtonTextColor(
        new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
        getAccentColor(),
        passwordDialog);
    passwordDialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                String captionText = captionEditText.getText().toString();
                boardID = captionText;
                postToPinterest(boardID);
                passwordDialog.dismiss();
              }
            });
  }

  private void postToPinterest(final String boardID) {
    SnackBarHandler.create(parent, getString(R.string.pinterest_image_uploading)).show();
    NotificationHandler.make(
        R.string.pinterest, R.string.upload_progress, R.drawable.ic_cloud_upload_black_24dp);
    Bitmap image = getBitmapFromPath(saveFilePath);
    PDKClient.getInstance()
        .createPin(
            caption,
            boardID,
            image,
            null,
            new PDKCallback() {
              @Override
              public void onSuccess(PDKResponse response) {
                NotificationHandler.actionPassed(R.string.upload_complete);
                Log.d(getClass().getName(), response.getData().toString());
                SnackBarHandler.create(parent, getString(R.string.pinterest_post)).show();
                sendResult(Constants.SUCCESS);
              }

              @Override
              public void onFailure(PDKException exception) {
                NotificationHandler.actionFailed();
                Log.e(getClass().getName(), exception.getDetailMessage());
                SnackBarHandler.create(parent, getString(R.string.Pinterest_fail)).show();
                sendResult(FAIL);
              }
            });
  }

  private void shareToTwitter() {
    if (Utils.checkAlreadyExist(TWITTER)) {
      Glide.with(this)
          .asBitmap()
          .load(Uri.fromFile(new File(saveFilePath)))
          .into(
              new CustomTarget<Bitmap>(1024, 512) {
                @Override
                public void onResourceReady(
                    @NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                  finalBmp = resource;
                  new PostToTwitterAsync().execute();
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}
              });
    } else {
      SnackBarHandler.create(parent, getString(R.string.sign_from_account)).show();
    }
  }

  private void uploadOnTwitter(String token, String secret) {
    SnackBarHandler.create(parent, getString(R.string.twitter_uploading)).show();
    final File f3 = new File(Environment.getExternalStorageDirectory() + "/twitter_upload/");
    final File file =
        new File(Environment.getExternalStorageDirectory() + "/twitter_upload/" + "temp" + ".png");
    if (!f3.exists()) f3.mkdirs();
    OutputStream outStream;
    try {
      outStream = new FileOutputStream(file);
      finalBmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
      outStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    String finalFile = file.getAbsolutePath();
    HelperMethods.postToTwitterWithImage(
        context,
        finalFile,
        caption,
        token,
        secret,
        new HelperMethods.TwitterCallback() {
          @Override
          public void onFinsihed(Boolean response) {
            isPostedOnTwitter = response;
            file.delete();
          }
        });
  }

  private void shareToOthers() {
    Uri uri = Uri.fromFile(new File(saveFilePath));
    Intent shareIntent = new Intent();
    shareIntent.setAction(Intent.ACTION_SEND);
    shareIntent.putExtra(Intent.EXTRA_TEXT, caption);
    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
    shareIntent.setType("image/png");
    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_image)));
    triedUploading = true;
    sendResult(SUCCESS);
  }

  private void copyCaption() {

    final AlertDialog.Builder dialogBuilder =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    String msg = getString(R.string.copy_caption);
    AlertDialogsHelper.getTextDialog(SharingActivity.this, dialogBuilder, R.string.caption, 0, msg);
    dialogBuilder.setPositiveButton(
        R.string.answer_yes,
        new DialogInterface.OnClickListener() {

          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            // copied caption to clipboard
            ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("caption", caption);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(SharingActivity.this, R.string.caption_copied, Toast.LENGTH_LONG).show();

            shareToInstagram();
          }
        });
    dialogBuilder.setNegativeButton(
        R.string.answer_no,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            shareToInstagram();
          }
        });
    AlertDialog alertDialog = dialogBuilder.create();
    alertDialog.show();
    AlertDialogsHelper.setButtonTextColor(
        new int[] {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
        getAccentColor(),
        alertDialog);
  }

  private void shareToMessenger() {
    Uri uri = Uri.fromFile(new File(saveFilePath));
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setPackage("com.facebook.orca");
    share.putExtra(Intent.EXTRA_STREAM, uri);
    share.setType("image/*");
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(share, caption));
    triedUploading = true;
    sendResult(SUCCESS);
  }

  private void shareToInstagram() {
    Uri uri = Uri.fromFile(new File(saveFilePath));
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setPackage("com.instagram.android");
    share.putExtra(Intent.EXTRA_STREAM, uri);
    share.setType("image/*");
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(share, caption));
    triedUploading = true;
    sendResult(SUCCESS);
  }

  private void shareToWhatsapp() {
    Uri uri = Uri.fromFile(new File(saveFilePath));
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    share.setPackage("com.whatsapp");
    share.putExtra(Intent.EXTRA_STREAM, uri);
    share.setType("image/*");
    share.putExtra(Intent.EXTRA_TEXT, caption);
    startActivityForResult(
        Intent.createChooser(share, context.getString(R.string.whatsapp)), SHARE_WHATSAPP);
    triedUploading = true;
    sendResult(SUCCESS);
  }

  private void shareToImgur() {
    final AlertDialog.Builder dialogBuilder =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    RealmQuery<AccountDatabase> query = realm.where(AccountDatabase.class);
    query.equalTo("name", getString(R.string.imgur));
    final RealmResults<AccountDatabase> result = query.findAll();
    if (result.size() != 0) {
      isPersonal = true;
      imgurAuth = Constants.IMGUR_HEADER_USER + " " + result.get(0).getToken();
    }
    AlertDialogsHelper.getTextDialog(
        SharingActivity.this, dialogBuilder, R.string.choose, R.string.imgur_select_mode, null);
    dialogBuilder.setPositiveButton(
        getString(R.string.personal).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            if (!isPersonal) {
              SnackBarHandler.create(
                      parent, getString(R.string.sign_from_account), Snackbar.LENGTH_SHORT)
                  .show();
              return;
            } else {
              isPersonal = true;
              uploadImgur();
            }
          }
        });

    dialogBuilder.setNeutralButton(
        getString(R.string.anonymous).toUpperCase(),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            isPersonal = false;
            uploadImgur();
          }
        });
    dialogBuilder.setNegativeButton(getString(R.string.exit).toUpperCase(), null);
    AlertDialog alertDialog = dialogBuilder.create();
    alertDialog.show();
    AlertDialogsHelper.setButtonTextColor(
        new int[] {
          DialogInterface.BUTTON_POSITIVE,
          DialogInterface.BUTTON_NEGATIVE,
          DialogInterface.BUTTON_NEUTRAL
        },
        getAccentColor(),
        alertDialog);
  }

  void uploadImgur() {
    final AlertDialog.Builder dialogBuilder =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    final AlertDialog dialog;
    final AlertDialog.Builder progressDialog =
        new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
    dialog =
        AlertDialogsHelper.getProgressDialog(
            SharingActivity.this,
            progressDialog,
            getString(R.string.posting_on_imgur),
            getString(R.string.please_wait));
    dialog.show();
    Bitmap bitmap = getBitmapFromPath(saveFilePath);
    final String imageString = getStringImage(bitmap);
    // sending image to server
    StringRequest request =
        new StringRequest(
            Request.Method.POST,
            Constants.IMGUR_IMAGE_UPLOAD_URL,
            new Response.Listener<String>() {
              @Override
              public void onResponse(String s) {
                dialog.dismiss();
                JSONObject jsonObject = null;

                try {
                  jsonObject = new JSONObject(s);
                  Boolean success = jsonObject.getBoolean("success");
                  if (success) {
                    final String url = jsonObject.getJSONObject("data").getString("link");

                    if (isPersonal) {
                      imgurString = getString(R.string.upload_personal) + "\n" + url;
                    } else {
                      imgurString = getString(R.string.upload_anonymous) + "\n" + url;
                    }

                    AlertDialogsHelper.getTextDialog(
                        SharingActivity.this,
                        dialogBuilder,
                        R.string.imgur_uplaoded_dialog_title,
                        0,
                        imgurString);
                    dialogBuilder.setPositiveButton(
                        getString(R.string.share).toUpperCase(),
                        new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialogInterface, int i) {
                            shareMsgOnIntent(SharingActivity.this, url);
                            sendResult(Constants.SUCCESS);
                          }
                        });

                    dialogBuilder.setNeutralButton(
                        getString(R.string.copy_action).toUpperCase(),
                        new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialogInterface, int i) {
                            copyToClipBoard(SharingActivity.this, url);
                            sendResult(Constants.SUCCESS);
                          }
                        });
                    dialogBuilder.setNegativeButton(getString(R.string.exit).toUpperCase(), null);
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.show();
                    AlertDialogsHelper.setButtonTextColor(
                        new int[] {
                          DialogInterface.BUTTON_POSITIVE,
                          DialogInterface.BUTTON_NEGATIVE,
                          DialogInterface.BUTTON_NEUTRAL
                        },
                        getAccentColor(),
                        alertDialog);
                  } else {
                    SnackBarHandler.create(parent, getString(R.string.error_on_imgur)).show();
                    sendResult(FAIL);
                  }
                } catch (JSONException e) {
                  e.printStackTrace();
                }
              }
            },
            new Response.ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError volleyError) {
                dialog.dismiss();
                SnackBarHandler.create(parent, getString(R.string.error_volly))
                    .show(); // add volleyError to check error
              }
            }) {
          @Override
          protected Map<String, String> getParams() throws AuthFailureError {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("image", imageString);
            if (caption != null && !caption.isEmpty()) parameters.put("title", caption);
            return parameters;
          }

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> headers = new HashMap<String, String>();
            if (isPersonal) {
              if (imgurAuth != null) {
                headers.put(getString(R.string.header_auth), imgurAuth);
              }
            } else {
              headers.put(getString(R.string.header_auth), getClientAuth());
            }

            return headers;
          }
        };
    request.setRetryPolicy(
        new DefaultRetryPolicy(50000, 5, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    RequestQueue rQueue = Volley.newRequestQueue(SharingActivity.this);
    rQueue.add(request);
  }

  /**
   * Function to share on NextCloud and OwnCloud because they share the common android library
   *
   * @param str the name of the account to upload
   */
  void shareToNextCloudAndOwnCloud(String str) {
    RealmQuery<AccountDatabase> query = realm.where(AccountDatabase.class);
    RealmResults<AccountDatabase> result = query.equalTo("name", str.toUpperCase()).findAll();

    if (result.size() != 0) {
      Uri serverUri = Uri.parse(result.get(0).getServerUrl());
      String username = result.get(0).getUsername();
      String password = result.get(0).getPassword();

      mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true);
      mClient.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(username, password));

      AssetManager assets = getAssets();
      try {
        String sampleFileName = getString(R.string.sample_file_name);
        File upFolder = new File(getCacheDir(), getString(R.string.upload_folder_path));
        upFolder.mkdir();
        File upFile = new File(upFolder, sampleFileName);
        FileOutputStream fos = new FileOutputStream(upFile);
        InputStream is = assets.open(sampleFileName);
        int count = 0;
        byte[] buffer = new byte[1024];
        while ((count = is.read(buffer, 0, buffer.length)) >= 0) {
          fos.write(buffer, 0, count);
        }
        is.close();
        fos.close();
      } catch (IOException e) {
        SnackBarHandler.create(parent, getString(R.string.error_copying_sample_file)).show();
        Log.e(LOG_TAG, getString(R.string.error_copying_sample_file), e);
      }

      File fileToUpload = new File(saveFilePath);
      String remotePath = FileUtils.PATH_SEPARATOR + fileToUpload.getName();
      ContentResolver cR = context.getContentResolver();
      MimeTypeMap mime = MimeTypeMap.getSingleton();
      Uri uri = Uri.fromFile(new File(saveFilePath));
      String type = mime.getExtensionFromMimeType(cR.getType(uri));
      String mimeType = type;

      // Get the last modification date of the file from the file system
      Long timeStampLong = fileToUpload.lastModified() / 1000;
      String timeStamp = timeStampLong.toString();

      UploadFileRemoteOperation uploadOperation =
          new UploadFileRemoteOperation(
              fileToUpload.getAbsolutePath(), remotePath, mimeType, timeStamp);
      uploadOperation.execute(mClient, this, mHandler);
      phimpmeProgressBarHandler.show();

    } else {
      SnackBarHandler.create(
              parent,
              context.getString(R.string.please_sign_into)
                  + str
                  + context.getString(R.string.from_account_manager))
          .show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int responseCode, Intent data) {
    super.onActivityResult(requestCode, responseCode, data);
    if (requestCode == REQ_SELECT_PHOTO) {
      if (responseCode == RESULT_OK) {
        NotificationHandler.actionPassed(R.string.upload_complete);
        SnackBarHandler.create(parent, getString(R.string.success_google), Snackbar.LENGTH_LONG)
            .show();
        sendResult(SUCCESS);
        return;
      } else {
        NotificationHandler.actionFailed();
        SnackBarHandler.create(parent, getString(R.string.error_google), Snackbar.LENGTH_LONG)
            .show();
        sendResult(FAIL);
        return;
      }
    }
    if (requestCode == REQ_CODE_SPEECH_INPUT && data != null) {
      ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
      String voiceInput = result.get(0);
      text_caption.setText(voiceInput);
      caption = voiceInput;
      return;
    }
    if (requestCode == SHARE_WHATSAPP) {
      if (responseCode == RESULT_OK) {
        triedUploading = true;
        sendResult(SUCCESS);
      } else sendResult(FAIL);
    }
  }

  private void goToHome() {
    Intent home = new Intent(SharingActivity.this, LFMainActivity.class);
    startActivity(home);
    finish();
  }

  @Override
  public void onResume() {
    ActivitySwitchHelper.setContext(this);
    super.onResume();
  }

  private void startRefresh() {
    ReadFileRemoteOperation refreshOperation =
        new ReadFileRemoteOperation(FileUtils.PATH_SEPARATOR);
    refreshOperation.execute(mClient, this, mHandler);
  }

  /**
   * Callback for Nextcloud operation
   *
   * @param operation
   * @param result result of success or failure
   */
  @Override
  public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
    phimpmeProgressBarHandler.hide();
    if (!result.isSuccess()) {
      Snackbar.make(parent, R.string.login_again, Snackbar.LENGTH_LONG)
          .setAction(
              R.string.exit,
              new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                  goToHome();
                }
              })
          .show();
    } else if (result.isSuccess()) {
      SnackBarHandler.create(
              parent, getString(R.string.todo_operation_finished_in_success), Snackbar.LENGTH_LONG)
          .show();
    } else if (operation instanceof UploadFileRemoteOperation) {
      onSuccessfulUpload();
    }
  }

  private void onSuccessfulUpload() {
    startRefresh();
  }

  public void setUpRecyclerView() {
    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
    shareAccountRecyclerView.setLayoutManager(layoutManager);
    shareAccountRecyclerView.setAdapter(shareAdapter);
    shareAccountRecyclerView.addOnItemTouchListener(new RecyclerItemClickListner(this, this));
  }

  private class PostToTwitterAsync extends AsyncTask<Void, Void, Void> {
    String token, secret;

    @Override
    protected void onPreExecute() {
      NotificationHandler.make(
          R.string.twitter, R.string.upload_progress, R.drawable.ic_cloud_upload_black_24dp);
      RealmQuery<AccountDatabase> query = realm.where(AccountDatabase.class);
      query.equalTo("name", TWITTER.toString());
      final RealmResults<AccountDatabase> result = query.findAll();
      if (result.size() != 0) {
        token = result.get(0).getToken();
        secret = result.get(0).getSecret();
      }
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      uploadOnTwitter(token, secret);
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      if (isPostedOnTwitter) {
        NotificationHandler.actionPassed(R.string.upload_complete);
        SnackBarHandler.create(parent, getString(R.string.tweet_posted_on_twitter)).show();
        sendResult(SUCCESS);
      } else {
        NotificationHandler.actionFailed();
        SnackBarHandler.create(parent, getString(R.string.error_on_posting_twitter)).show();
        sendResult(FAIL);
      }
    }
  }

  private class PostToTumblrAsync extends AsyncTask<Void, Void, Void> {
    AlertDialog dialog;
    TumblrClient tumblrClient;
    JumblrClient client;
    Boolean success = true;

    @Override
    protected void onPreExecute() {
      tumblrClient = new TumblrClient();
      AlertDialog.Builder progressDialog =
          new AlertDialog.Builder(SharingActivity.this, getDialogStyle());
      dialog =
          AlertDialogsHelper.getProgressDialog(
              SharingActivity.this,
              progressDialog,
              getString(R.string.posting_tumblr),
              getString(R.string.please_wait));
      dialog.show();
      client = tumblrClient.getClient();
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      User user = client.user();
      PhotoPost post = null;
      try {
        post = client.newPost(user.getBlogs().get(0).getName(), PhotoPost.class);
        if (caption != null && !caption.isEmpty()) post.setCaption(caption);
        post.setData(new File(saveFilePath));
        post.save();
      } catch (IllegalAccessException | InstantiationException e) {
        success = false;
        e.printStackTrace();
      } catch (IOException e) {
        success = false;
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      dialog.dismiss();
      if (success) {
        SnackBarHandler.create(parent, getString(R.string.posted_on_tumblr)).show();
        sendResult(Constants.SUCCESS);
      } else {
        SnackBarHandler.create(parent, getString(R.string.error_on_tumblr)).show();
        sendResult(FAIL);
      }
    }
  }
}
