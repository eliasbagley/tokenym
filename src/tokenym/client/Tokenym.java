package tokenym.client;

import java.awt.Checkbox;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import tokenym.shared.Utils;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

// TODO
// more robust password and email checker, to make sure they are valid
// refactor everything, and comment things nicely. make elegent code
// encrypt before sending to server, decrypt at server
// profile to find slow spots
// generate tokens and keyboards on client, and send them to the server so that there is less delay
// ads
// gwt log when failures occur
// add a different label for error messages, instead of just using the token field
// version number
// github/source control

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Tokenym implements EntryPoint
{
    /**
     * Create a remote service proxy to talk to the server-side Greeting service.
     */
    private final TokenymServiceAsync greetingService = GWT.create(TokenymService.class);

    /**
     * This is the entry point method.
     */
    public void onModuleLoad ()
    {
        // ListBox for mode selection
        final ListBox lb = new ListBox();
        lb.addItem("Basic");
        lb.addItem("Two-factor Grid Option");
        lb.addItem("Redeem Token");
        lb.setVisibleItemCount(3);
        
        // Buttons
        final Button sendButton = new Button("Enter");
        final Button newGridButton = new Button("Need a new grid?");
        final Button newKeyboardButton = new Button("Randomize Keyboard"); // not currently in use

        // checkbox for toggling between basic and advanced
        final CheckBox cb = new CheckBox("Two-factor Grid Option"); // not currently in use

        // textboxes
        final PasswordTextBox passwordField = new PasswordTextBox(); // enter the "tokenym", and get a token in return
        final TextBox emailField = new TextBox(); // where the user will enter the email address for getting the grid
        final TextBox tokenField = new TextBox(); // in V+, you need to chain a token along each time
        final TextBox registerEmailField = new TextBox(); // email field used in dialog box for registering

        // fields and labels
        final HTML tokenLabel = new HTML();
        final HTML keyboardLabel = new HTML(); // for showing the randomized keyboard

        // set the width of all fields to be the same
        passwordField.setWidth("250px");
        emailField.setWidth("250px");
        registerEmailField.setWidth("250px");
        tokenField.setWidth("250px");

        // placeholder strings
        tokenField.getElement().setPropertyString("placeholder", "Last Previous Token");
        passwordField.getElement().setPropertyString("placeholder", "Password");
        emailField.getElement().setPropertyString("placeholder", "Ledger Email Address");
        registerEmailField.getElement().setPropertyString("placeholder", "Email Addresss");

        // hide advanced features by default
        tokenField.setVisible(false);
        newGridButton.setVisible(false);
        emailField.setVisible(false);
        newKeyboardButton.setVisible(false);
        keyboardLabel.setVisible(false);

        // We can add style names to widgets
        sendButton.addStyleName("sendButton");

        // Add the nameField and sendButton to the RootPanel
        // Use RootPanel.get() to get the entire body element

        //RootPanel.get("checkboxContainer").add(cb);
        RootPanel.get("checkboxContainer").add(lb);
        RootPanel.get("tokenLabelContainer").add(tokenLabel);
        RootPanel.get("tokenFieldContainer").add(tokenField);
        RootPanel.get("emailFieldContainer").add(emailField);
        RootPanel.get("sendEmailButtonContainer").add(newGridButton);
        RootPanel.get("passwordFieldContainer").add(passwordField);
        RootPanel.get("sendButtonContainer").add(sendButton);
        RootPanel.get("keyboardLabelContainer").add(keyboardLabel);
        RootPanel.get("newKeyboardButtonContainer").add(newKeyboardButton);

        // Focus the cursor on the name field when the app loads
        passwordField.setFocus(true);

        // Create the popup dialog box
        final DialogBox dialogBox = new DialogBox();
        // dialogBox.setSize("200px", "200px");
        //dialogBox.setAnimationEnabled(true);

        final Button submitButton = new Button("Send");
        final Button closeButton = new Button("Cancel");

        // set button widths
        submitButton.setWidth("100px");
        closeButton.setWidth("100px");

        // We can set the id of a widget by accessing its Element
        closeButton.getElement().setId("closeButton");
        submitButton.getElement().setId("closeButton");
        registerEmailField.getElement().setId("registerEmailField");

        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.add(submitButton);
        hPanel.add(closeButton);

        VerticalPanel dialogVPanel = new VerticalPanel();
        dialogVPanel.addStyleName("dialogVPanel");
        dialogVPanel.add(registerEmailField);
        dialogVPanel.add(hPanel);

        dialogBox.setWidget(dialogVPanel);

        // Add a handler to close the DialogBox
        closeButton.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event)
            {
                
                dialogBox.hide();
            }
        });

        // Add a handler to close the DialogBox
        submitButton.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event)
            {
                sendEmail();
            }

            private void sendEmail ()
            {
                // First, we validate the input.
                String textToServer = registerEmailField.getText();

                // do additional email checking to make sure it is a well-formed string
                if (textToServer.equals("")) // don't try to send an email if they don't enter anything
                {
                    return;
                }

                greetingService.sendEmail(textToServer, new AsyncCallback<String>() {
                    public void onFailure (Throwable caught)
                    {
                        // Show the RPC error message to the user

                        dialogBox.setText("We're sorry, something went wrong. Please try again.");
                        dialogBox.center();
                        

                    }

                    public void onSuccess (String result)
                    {
                        // maybe do something more intuitive here?
                        dialogBox.hide();
                        
                    }
                });
            }

        });

        // Create a handler for the sendButton and nameField
        class SendPasswordHandler implements ClickHandler, KeyUpHandler
        {
            /**
             * Fired when the user clicks on the sendButton.
             */
            public void onClick (ClickEvent event)
            {
                    sendPasswordToServer();
            }

            /**
             * Fired when the user types in the nameField.
             */
            public void onKeyUp (KeyUpEvent event)
            {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
                {
                        sendPasswordToServer();
                }
            }

            /**
             * Send the name from the nameField to the server and wait for a response.
             * @throws UnsupportedEncodingException 
             * @throws NoSuchAlgorithmException 
             */
            private void sendPasswordToServer ()
            {
                // what about generating a token here, and passing it along, so things are faster?
                
                //tokenLabel.setHTML("here");
                String token = tokenField.getText(); //
                token = token.replaceAll(" ", ""); // strip all spaces from it to prevent problems
                // do some on the fly error checking: make sure it matches the format of a token
                String password = passwordField.getText();
                String keyboard = keyboardLabel.getText();
                String email = emailField.getText();
                
                // generate the token at the client now, and send it to the server
                final String generatedToken = Utils.createUniqueToken();
                
                // Then, we send the input to the server.
                // use the checkbox instead
                //Boolean gridOption = cb.getValue();
                int mode = lb.getSelectedIndex();
                if (mode == 0) // if no token is entered, use regular veridem
                {
                    
                    tokenLabel.setHTML("Token: " + generatedToken);
                    
                    // only hash on basic version, otherwise grid won't work
                    // because of double hashing, ID will not be same
                    // think about the hashing/encrypting later. consider how it will effect
                    // TokenService servlet
                    greetingService.getToken(password, generatedToken, new AsyncCallback<Boolean>() {
                        public void onFailure (Throwable caught)
                        {
                            tokenLabel.setHTML("Something went wrong. Please try again.");
                        }

                        public void onSuccess (Boolean validPassword)
                        {
                            if (!validPassword)
                                tokenLabel.setHTML("Password must be 8 characters, with uppercase, lowercase, numeric, and symbol characters.");
                        }
                    });
                }
                else if(mode == 1)// use grid method
                {
                    // don't send stuff to server if they don't type anything in
                    // they must have all three things entered for it to work
                    if (password.length() == 0 || token.length() == 0 || email.length() == 0)
                    {
                        tokenLabel.setHTML("Enter information in all fields.");
                         //why was I clearing out their old progress
                        //passwordField.setText("");
                        //tokenField.setText("");
                        //emailField.setText("");
                        return;
                    }
                    
                    passwordField.setText("Token: " + generatedToken); // display the generated token before the server call
                    // consider security of doing this.. can a client spoof a token?
                    
                    greetingService.veridemPlusGetToken(token, password, keyboard, email, generatedToken, new AsyncCallback<Boolean>() {
                        public void onFailure (Throwable caught)
                        {
                            // Show the RPC error message to the user

                            tokenLabel.setHTML("Something went wrong. Please try again.");
                        }

                        // returns a new token on success
                        public void onSuccess (Boolean validPassword)
                        {
                            if (!validPassword)
                            {
                                tokenLabel.setHTML("Password must be 8 characters, with uppercase, lowercase, numeric, and symbol characters.");
                            }
                            else
                            {
                                tokenLabel.setHTML("Token: " + generatedToken);
                                //tokenField.setText(""); // only clear out their previous token when we generate a new token for them
                                // set the token field to be the new token they get
                                tokenField.setText(generatedToken);
                            }
                            //  tokenLabel.setHTML(result);
                            
                        }
                    });
                }
                else if(mode == 2)
                {
                    // redeem token module
                    
                    greetingService.getID(token, new AsyncCallback<String>() {
                        public void onFailure (Throwable caught)
                        {
                          tokenLabel.setHTML("Something went wrong. Please try again.");
                        }

                        public void onSuccess (String result)
                        {
                            tokenLabel.setHTML("ID: " + result);
                            tokenField.setText(""); 
                        }
                    });
                    
                }
                
                // reset all the text in the fields after a send event
                passwordField.setText(""); // remove the password, so it can't be pressed again
                // moved the clear token field to the onsuccess and on failure
                //emailField.setText(""); // no reason to make them enter an email address twice
                // generate a new keyboard
                newKeyboardButton.click();
            }
        }

        class RegistrationHandler implements ClickHandler
        {
            /**
             * Fired when the user clicks on the sendButton.
             */
            public void onClick (ClickEvent event)
            {
                // clear the text before showing the dialog box
                registerEmailField.setText("");
                dialogBox.setText("");
                dialogBox.center();
                
            }
        }

        class NewKeyboardHandler implements ClickHandler
        {
            /**
             * Fired when the user clicks on the sendButton.
             */
            public void onClick (ClickEvent event)
            {
                genKeyboard();
            }

            private void genKeyboard ()
            {

                greetingService.generateNewKeyboard("wat", new AsyncCallback<String>() {
                    public void onFailure (Throwable caught)
                    {
                        keyboardLabel.setText("Please try again");
                    }

                    public void onSuccess (String result)
                    {
                        String formattedKeyboard = result.replace("\n", "<br>");

                        // split it by <br> tags
                        String[] splitKeyboard = formattedKeyboard.split("<br>");

                        // this block of code just puts spaces inbetween each character in the keyboard
                        String newKeyboard = "";
                        for (int i = 0; i < splitKeyboard.length; i++)
                        {
                            String temp = "";
                            String temp2 = splitKeyboard[i];
                            for (int j = 0; j < temp2.length(); j++)
                            {
                                temp = temp + temp2.charAt(j) + " ";
                                if (j == (Math.floor(temp2.length()/2)-1)) // add another space in the middle to separate
                                    temp +="&#160;";
                            }
                            newKeyboard = newKeyboard + temp + "<br>";
                            if (i == (Math.floor(splitKeyboard.length/2)-1)) // add another break for the middle to separate
                                newKeyboard += "<br>";
                        }

                        keyboardLabel.setHTML("<h1>" + newKeyboard + "</h1>");
                    }
                });
            }
        }

        class CheckBoxHandler implements ClickHandler
        {
            public void onClick (ClickEvent event)
            {
                // add a single click to generate a keyboard from the beginning
                newKeyboardButton.click();

                // set visibility of advanced features, depending on value of the checkbox
                Boolean visible = cb.getValue();

                tokenField.setVisible(visible);
                newGridButton.setVisible(visible);
                emailField.setVisible(visible);
                //newKeyboardButton.setVisible(visible); don't show the new keyboard button anymore
                keyboardLabel.setVisible(visible);

                // clear out all the fields upon changing
                tokenLabel.setText("");
                emailField.setText("");
                tokenField.setText("");
                passwordField.setText("");

                if (visible)
                    passwordField.getElement().setPropertyString("placeholder", "Grid Encrypted Password");
                else
                    passwordField.getElement().setPropertyString("placeholder", "Password");

            }
        }
        
        class ListBoxHandler implements ChangeHandler
        {
            public void onChange(ChangeEvent event)
            {
               // get the current value
                int index = lb.getSelectedIndex();
                
                // first, hide everything
                tokenField.setVisible(false);
                newGridButton.setVisible(false);
                emailField.setVisible(false);
                newKeyboardButton.setVisible(false);
                keyboardLabel.setVisible(false);
                passwordField.setVisible(false);
                
                // possibly temp: clear out token label
                tokenLabel.setText("");
                
                switch(index)
                {
                    // selectively show what needs to be shown for the given index
                    case 0:
                    {
                        passwordField.setVisible(true);
                        passwordField.getElement().setPropertyString("placeholder", "Password");
                        
                        // get a new keyboard
                        newKeyboardButton.click();
                        
                        break;
                        
                    }
                    case 1:
                    {
                        newGridButton.setVisible(true);
                        
                        tokenField.setVisible(true);
                        tokenField.getElement().setPropertyString("placeholder", "Last Previous Token");
                        
                        emailField.setVisible(true);
                        
                        passwordField.setVisible(true);
                        passwordField.getElement().setPropertyString("placeholder", "Grid Encrypted Password");
                        
                        keyboardLabel.setVisible(true);
                        
                        // don't generate a new keyboard here, so that it doesn't look choppy
                        break;
                    }
                    case 2:
                    {
                        tokenField.getElement().setPropertyString("placeholder", "Token");
                        tokenField.setVisible(true);
                        
                        // get a new keyboard
                        newKeyboardButton.click();
                        break;
                    }
                }
            }
        }

        // Add a handler to send the name to the server
        SendPasswordHandler handler = new SendPasswordHandler();
        sendButton.addClickHandler(handler);
        passwordField.addKeyUpHandler(handler);

        RegistrationHandler emailHandler = new RegistrationHandler();
        newGridButton.addClickHandler(emailHandler);

        // activates the "newkeyboard" button, so that when you press it, you get a new onscreen keyboard
        NewKeyboardHandler keyboardHandler = new NewKeyboardHandler();
        newKeyboardButton.addClickHandler(keyboardHandler);

        CheckBoxHandler cbHandler = new CheckBoxHandler();
        cb.addClickHandler(cbHandler);
        
        ListBoxHandler lbHandler = new ListBoxHandler();
        lb.addChangeHandler(lbHandler);

        // generate a keyboard for the first time. The rest of the randomization will be from changing indexes
        newKeyboardButton.click();
    }
}
