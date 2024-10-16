package de.time4innovation.plugins.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.NfcA;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

@CapacitorPlugin(name = "Time4InnovationNfc")
public class Time4InnovationNfcPlugin extends Plugin {

    private NfcAdapter nfcAdapter;
    private String callbackId;
    private static final String TAG = "Time4InnovationNfc";

    @Override
    public void load() {
        super.load();

        // Initialize NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(getContext());

        if (nfcAdapter == null) {
            notifyJavaScript("NFC is not available on this device.");
        } else if (!nfcAdapter.isEnabled()) {
            notifyJavaScript("Please enable NFC in your settings.");
        }

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
            String tagInfo = readFromTag(tag);
            Log.d(TAG, "handleOnNewIntent tag info " + tagInfo);

            notifyJavaScript(tagInfo);
        }
        // }
    }

    private String readFromTag(Tag tag) {
        Log.d(TAG, "readFromTag " + tag);
        StringBuilder stringBuilder = new StringBuilder();
        byte[] id = tag.getId();
        stringBuilder.append("Tag ID (hex): ").append(bytesToHex(id)).append("\n");

        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();

                if (ndefMessage != null) {
                    NdefRecord[] records = ndefMessage.getRecords();
                    for (NdefRecord ndefRecord : records) {
                        if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                            stringBuilder.append("Message: ").append(getTextFromNdefRecord(ndefRecord)).append("\n");
                        }
                    }
                }
                ndef.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            stringBuilder.append("NFC tech not supported");
        }

        return stringBuilder.toString();
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
    private void notifyJavaScript(String message) {
        Log.d(TAG, "notifyJavaScript");
        if (callbackId != null) {
            PluginCall savedCall = getBridge().getSavedCall(callbackId);
            if (savedCall != null) {
                JSObject result = new JSObject();
                result.put("tagInfo", message);
                savedCall.resolve(result);
            }
        }
    }

    @PluginMethod
    public void initializeNFC(PluginCall call) {
        Log.d(TAG, "initializeNFC");
        // Save the callbackId for future NFC detections
        callbackId = call.getCallbackId();
        call.save(); // Save the call in case we need to resolve it later
    }
}
