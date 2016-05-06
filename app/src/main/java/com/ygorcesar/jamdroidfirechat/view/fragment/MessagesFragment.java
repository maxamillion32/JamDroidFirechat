package com.ygorcesar.jamdroidfirechat.view.fragment;

import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.ygorcesar.jamdroidfirechat.R;
import com.ygorcesar.jamdroidfirechat.databinding.FragmentMessagesBinding;
import com.ygorcesar.jamdroidfirechat.model.Message;
import com.ygorcesar.jamdroidfirechat.model.User;
import com.ygorcesar.jamdroidfirechat.utils.Constants;
import com.ygorcesar.jamdroidfirechat.utils.ConstantsFirebase;
import com.ygorcesar.jamdroidfirechat.utils.Utils;
import com.ygorcesar.jamdroidfirechat.view.adapters.MessageItemAdapter;
import com.ygorcesar.jamdroidfirechat.viewmodel.MessageAdapterViewModelContract;
import com.ygorcesar.jamdroidfirechat.viewmodel.MessageFragmViewModel;
import com.ygorcesar.jamdroidfirechat.viewmodel.MessageFragmViewModelContract;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment implements MessageFragmViewModelContract,
        MessageAdapterViewModelContract {

    private String mEncodedMail;
    private List<User> mUsers;
    private List<String> mUsersEmails;
    private List<Message> mMessages;
    private List<String> mKeys;
    private Firebase mRefMessages;
    private Firebase mRefUsers;
    private ChildEventListener childMessagesListener;
    private ValueEventListener valueUserListener;
    private String mChildChatKey;
    private String mUserOneSignalID;
    private FragmentMessagesBinding mFragmentMessagesBinding;
    private static final String TAG = "MessagesFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragmentMessagesBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_messages, container, false);


        if (getArguments() != null) {
            mChildChatKey = getArguments().getString(Constants.KEY_CHAT_CHILD, "");
            mUserOneSignalID = getArguments().getString(Constants.KEY_USER_ONE_SIGNAL_ID, "");
            getActivity().setTitle(getArguments()
                    .getString(Constants.KEY_USER_DISPLAY_NAME, getString(R.string.app_name)));
        }

        initializeScreen();
        return mFragmentMessagesBinding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeFirebase();
    }

    @Override
    public void onPause() {
        super.onPause();
        removeFirebaseListeners();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().setTitle(getString(R.string.app_name));
    }

    /**
     * Inicializando Adapters, RecyclerView e Listeners...
     */
    private void initializeScreen() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mEncodedMail = prefs.getString(Constants.KEY_ENCODED_EMAIL, "");

        mUsers = new ArrayList<>();
        mUsersEmails = new ArrayList<>();
        mMessages = new ArrayList<>();
        mKeys = new ArrayList<>();

        MessageItemAdapter adapter = new MessageItemAdapter(this, mEncodedMail, mUsersEmails);
        mFragmentMessagesBinding.rvMessage.setAdapter(adapter);
        adapter.setMessages(mMessages);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String loggedUserName = preferences.getString(Constants.KEY_USER_DISPLAY_NAME, "");

        mFragmentMessagesBinding.rvMessage.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFragmentMessagesBinding.setMessageViewModel(new MessageFragmViewModel(getActivity(), this,
                mEncodedMail, mChildChatKey, mUserOneSignalID, loggedUserName));
    }

    /**
     * Inicializando Firebase e Listeners do Firebase
     */
    private void initializeFirebase() {
        if (childMessagesListener == null && valueUserListener == null) {
            valueUserListener = createFirebaseUsersListeners();
            childMessagesListener = createFirebaseChatListener();
        }
        mRefUsers = new Firebase(ConstantsFirebase.FIREBASE_URL).child(ConstantsFirebase.FIREBASE_LOCATION_USERS);
        mRefUsers.addValueEventListener(valueUserListener);


        mRefMessages = new Firebase(ConstantsFirebase.FIREBASE_URL_CHAT).child(mChildChatKey);
        mRefMessages.keepSynced(true);
        Query chatsRef = mRefMessages.orderByKey().limitToLast(50);

        chatsRef.addChildEventListener(childMessagesListener);
    }

    /**
     * Criando Listener para observar nó messagens de determinado chat
     *
     * @return
     */
    private ChildEventListener createFirebaseChatListener() {
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    Message message = dataSnapshot.getValue(Message.class);
                    mMessages.add(message);
                    mKeys.add(dataSnapshot.getKey());

                    int posAdded = mMessages.size() - 1;
                    mFragmentMessagesBinding.rvMessage.scrollToPosition(posAdded);
                    mFragmentMessagesBinding.rvMessage.getAdapter().notifyItemInserted(posAdded);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    int index = mKeys.indexOf(dataSnapshot.getKey());
                    if (index != -1) {
                        mMessages.set(index, dataSnapshot.getValue(Message.class));
                        mFragmentMessagesBinding.rvMessage.getAdapter().notifyItemChanged(index);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    int index = mKeys.indexOf(dataSnapshot.getKey());
                    if (index != -1) {
                        mMessages.remove(index);
                        mKeys.remove(index);
                        mFragmentMessagesBinding.rvMessage.getAdapter().notifyItemRemoved(index);
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };
    }

    private ValueEventListener createFirebaseUsersListeners() {
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        mUsers.add(snapshot.getValue(User.class));
                        mUsersEmails.add(snapshot.getValue(User.class).getEmail());
                    }
                    ((MessageItemAdapter) mFragmentMessagesBinding.rvMessage.getAdapter()).setUsers(mUsers);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        };
    }

    /**
     * Remove listeners dos objetos Firebase
     */
    private void removeFirebaseListeners() {
        if (childMessagesListener != null && valueUserListener != null) {
            mRefMessages.removeEventListener(childMessagesListener);
            mRefUsers.removeEventListener(valueUserListener);
        }
    }

    /**
     * Resgata click do adapter por meio da interface implementada para exibir usuário da mensagem
     *
     * @param user
     */
    @Override
    public void onMessageItemClick(User user) {
//        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        FragmentManager fragmentManager = getFragmentManager();
        Bundle args = new Bundle();
        args.putString(Constants.KEY_USER_DISPLAY_NAME, user.getName());
        args.putString(Constants.KEY_ENCODED_EMAIL, Utils.decodeEmail(user.getEmail()));
        args.putString(Constants.KEY_USER_PROVIDER_PHOTO_URL, user.getPhotoUrl());
        UserFragment fragment = new UserFragment();
        fragment.setArguments(args);
        fragment.show(fragmentManager, "fragment_user");

//        transaction.replace(R.id.fragment, fragment)
//                .addToBackStack(null)
//                .commit();
    }

    @Override
    public void setEditTextMessage(String msg) {
        mFragmentMessagesBinding.edtMessageContent.setText(msg);
    }

    @Override
    public void showToastMessage(int string_res) {
        Toast.makeText(getActivity(), getString(string_res), Toast.LENGTH_SHORT).show();
    }

    @Override
    public String getEditTextMessage() {
        return mFragmentMessagesBinding.edtMessageContent.getText().toString();
    }
}
