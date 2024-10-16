package de.time4innovation.plugins.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.provider.Settings;
import android.widget.Toast;

@CapacitorPlugin(name = "Time4InnovationNfc")
public class Time4InnovationNfcPlugin extends Plugin {

    private NfcAdapter nfcAdapter;
    private String callbackId;
    private static final String TAG = "Time4InnovationNfc";

    @Override
    public void load() {
        super.load();

        checkNfcPermissions();

        Log.d(TAG, "nfc ok " + nfcAdapter);
    }

    @Override
    public void handleOnResume() {
        Log.d(TAG, "handleOnResume");
        super.handleOnResume();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            enableForegroundDispatch();
        }
    }

    @Override
    public void handleOnPause() {
        Log.d(TAG, "handleOnPause");
        super.handleOnPause();
        if (nfcAdapter != null) {
            disableForegroundDispatch();
        }
    }

    private void enableForegroundDispatch() {
        Log.d(TAG, "enableForegroundDispatch");
        Intent intent = new Intent(getContext(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_MUTABLE);

        IntentFilter[] intentFilters = new IntentFilter[] {};
        String[][] techList = new String[][] {
                { Ndef.class.getName() }
        };

        nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, intentFilters, techList);
    }

    private void disableForegroundDispatch() {
        Log.d(TAG, "disableForegroundDispatch");
        nfcAdapter.disableForegroundDispatch(getActivity());
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        Log.d(TAG, "handleOnNewIntent");
        super.handleOnNewIntent(intent);

        Log.d(TAG, "handleOnNewIntent ACTION_TAG_DISCOVERED " + NfcAdapter.ACTION_TAG_DISCOVERED);
        Log.d(TAG, "handleOnNewIntent intent action " + intent.getAction());

        // if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "handleOnNewIntent tag " + tag);

        if (tag != null) {
            JSObject tagDetails = readFromTag(tag);
            Log.d(TAG, "handleOnNewIntent tag info " + tagDetails);

            notifyJavaScript(tagDetails);
        }
        // }
    }

    private JSObject readFromTag(Tag tag) {
        Log.d(TAG, "readFromTag " + tag);

        JSObject tagDetails = new JSObject();

        // prepare tag id
        byte[] id = tag.getId();
        String tagId = bytesToHex(id);

        tagDetails.put("tagId", tagId);

        // prepare tag messages
        Ndef ndef = Ndef.get(tag);
        JSONArray messagesArray = new JSONArray(); // Array to store messages
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();

                if (ndefMessage != null) {
                    NdefRecord[] records = ndefMessage.getRecords();
                    for (NdefRecord ndefRecord : records) {
                        if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                            String message = getTextFromNdefRecord(ndefRecord); // Extract the message
                            messagesArray.put(message); // Add each message to the array
                        }
                    }
                }
                ndef.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        tagDetails.put("messages", messagesArray);

        return tagDetails;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private String getTextFromNdefRecord(NdefRecord ndefRecord) throws UnsupportedEncodingException {
        byte[] payload = ndefRecord.getPayload();

        // Get the Text Encoding
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

        // Get the Language Code
        int languageCodeLength = payload[0] & 0063;

        // Get the Text
        return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
    }

    // Notify JavaScript through Capacitor Plugin
    private void notifyJavaScript(JSObject tagDetails) {
        Log.d(TAG, "notifyJavaScript");
        if (callbackId != null) {
            PluginCall savedCall = getBridge().getSavedCall(callbackId);
            if (savedCall != null) {
                savedCall.resolve(tagDetails);
            }
        }
    }

    private void checkNfcPermissions() {
        // Initialize the NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(getContext());

        // Check if NFC is available
        if (nfcAdapter == null) {
            Toast.makeText(this.getContext(), R.string.nfc_not_available, Toast.LENGTH_LONG).show();
            return;
        }

        // Check if NFC is enabled
        if (!nfcAdapter.isEnabled()) {
            // Prompt user to enable NFC
            showEnableNfcDialog();
        }
    }

    private void showEnableNfcDialog() {
        // Create a dialog to prompt user to enable NFC
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setMessage(R.string.nfc_disabled)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Open NFC settings
                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getContext().startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @PluginMethod
    public void initializeNFC(PluginCall call) {
        Log.d(TAG, "initializeNFC");
        // Save the callbackId for future NFC detections
        callbackId = call.getCallbackId();
        call.setKeepAlive(true); // Save the call in case we need to resolve it later
    }
}
