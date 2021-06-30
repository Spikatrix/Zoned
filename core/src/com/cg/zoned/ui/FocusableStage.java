package com.cg.zoned.ui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The FocusableStage is basically a Stage with focus managing capabilities on Actors
 * This means that it can track and focus actors when the user navigates using the keyboard
 *
 * <p>
 * Custom behavior implemented for: <br>
 * - TextField <br>
 * - SelectBox <br>
 *
 * <p>
 * Currently supported keyboard inputs include: <br>
 * - TAB            for focusing the next Actor <br>
 * - SHIFT + TAB    for focusing the previous Actor <br>
 * - LEFT           for focusing the previous Actor <br>
 * - RIGHT          for focusing the next Actor <br>
 * - UP             for focusing the above Actor <br>
 * - DOWN           for focusing the below Actor <br>
 * - Enter or Space for pressing the currently focused Actor <br>
 * provided the currently focused actor is not busy
 *
 * @author Spikatrix
 */

public class FocusableStage extends Stage {
    /**
     * Stores all the focusable actors into an Array for navigating through it
     * The same Actor object is stored colspan times so as to satisfy the grid arrangement properly
     * A null value in the array indicates a row in the actor arrangement
     */
    private Array<Actor> focusableActorArray = new Array<>();

    /**
     * Holds the currently focused actor
     */
    private Actor currentFocusedActor;

    /**
     * Holds the actor that is held down
     */
    private Actor downActor;

    /**
     * Active dialog list
     */
    private Array<Dialog> dialogs = new Array<>();

    /**
     * The Texture used in the background of a dialog when it is active
     */
    private Texture dialogBackgroundTexture = null;

    /**
     * Master switch for enabling keyboard based focus management
     * When this is false, this class will mostly behave like a regular Stage
     */
    private boolean isActive = true;

    /**
     * If set, the scrollpane will scroll focused actors into view
     */
    private ScrollPane scrollpane;

    /**
     * The skin used for styling dialog elements
     */
    private Skin skin;

    /**
     * Scale factor used for scaling dialog UI elements accordingly
     */
    private float scaleFactor;


    public FocusableStage(Viewport viewport) {
        this(viewport, 1f);
    }

    public FocusableStage(Viewport viewport, float scaleFactor) {
        this(viewport, scaleFactor, null);
    }

    public FocusableStage(Viewport viewport, float scaleFactor, Skin skin) {
        super(viewport);

        this.scaleFactor = scaleFactor;
        this.skin = skin;

        createDialogBGTexture();
    }

    public void setScrollFocus(ScrollPane scrollPane) {
        super.setScrollFocus(scrollPane);
        this.scrollpane = scrollPane;
    }

    /**
     * Creates the dialog background texture
     */
    private void createDialogBGTexture() {
        if (dialogBackgroundTexture != null) {
            dialogBackgroundTexture.dispose();
        }

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA4444);
        pixmap.setColor(new Color(0, 0, 0, .8f));
        pixmap.drawPixel(0, 0);
        dialogBackgroundTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * Method for adding a focusable Actor with a colspan of 1
     *
     * @param actor The actor for tracking focus
     */
    public void addFocusableActor(Actor actor) {
        addFocusableActor(actor, 1);
    }

    /**
     * Method for adding a focusable Actor with the specified colspan
     *
     * @param actor   The actor for tracking focus
     * @param colspan The number of columns that the Actor spans across
     */
    public void addFocusableActor(Actor actor, int colspan) {
        while (colspan-- > 0) {
            focusableActorArray.add(actor);

            if (actor instanceof TextField) {
                final int TAB = 9; // 9 is the ASCII code for a TAB character
                /*
                 * Disable TextField's default TAB and SHIFT + TAB focus navigation
                 * to prevent it from interfering with our custom focus implementation
                 */
                ((TextField) actor).setFocusTraversal(false);
                ((TextField) actor).setOnlyFontChars(true);
                ((TextField) actor).setTextFieldListener((textField, typedChar) -> {
                    /*
                     * Gets rid of those pesky tabs that get inserted into the TextField
                     * when tab navigating through them
                     */
                    if (typedChar == TAB) {
                        textField.setText(textField.getText().replaceAll("\t", ""));
                    }
                });
            }
        }
    }

    /**
     * Method for indicating the end of a row
     */
    public void row() {
        focusableActorArray.add(null);
    }

    /**
     * Method to focus an Actor that was added into the Array
     * It first defocuses the currently focused Actor and then focuses the new Actor
     *
     * @param actor The Actor to focus
     * @return true if the Actor was not null and is present in the Array
     * false otherwise
     */
    public boolean setFocusedActor(Actor actor) {
        if (actor != null && focusableActorArray.contains(actor, true)) {
            defocus(currentFocusedActor); // Defocus currently focused actor
            focus(actor);                 // Focus new actor
            return true;
        }

        return false;
    }

    /**
     * Sends an enter event to focus an Actor
     *
     * @param actor The Actor to focus
     */
    private void focus(Actor actor) {
        InputEvent event = new InputEvent();
        event.setType(InputEvent.Type.enter);
        event.setPointer(-1);
        actor.fire(event);

        currentFocusedActor = actor;
        super.setKeyboardFocus(actor);
        super.setScrollFocus(actor);

        if (scrollpane != null && !dialogIsActive()) {
            scrollIntoView(actor);
        }
    }

    /**
     * Sends an exit event to defocus an Actor
     *
     * @param actor The Actor to defocus
     */
    private void defocus(Actor actor) {
        if (actor != null) {
            InputEvent event = new InputEvent();
            event.setType(InputEvent.Type.exit);
            event.setPointer(-1);
            actor.fire(event);

            currentFocusedActor = null;
            super.setKeyboardFocus(null);
            super.setScrollFocus(null);
        }
    }

    /**
     * Puts the specified actor into view by scrolling the set scrollpane
     *
     * @param actor The actor to scroll into view
     */
    private void scrollIntoView(Actor actor) {
        Actor parent = scrollpane.getActor();
        if (parent == null) {
            parent = scrollpane;
        }

        float xOffset = 0;
        float yOffset = 0;
        float width = actor.getWidth();
        float height = actor.getHeight();

        // Loop finds the X and Y offsets of the actor relative to the scrollpane
        do {
            yOffset += actor.getY();
            xOffset += actor.getX();
            actor = actor.getParent();
        } while (parent != actor && actor != null);

        scrollpane.scrollTo(xOffset, yOffset, width, height, true, true);
    }

    /**
     * Shows an dialog with focus properties on its buttons
     *
     * @param msg                   The message to display
     * @param buttons               Buttons to be displayed in the dialog
     * @param useVerticalButtonList Determines whether the dialog buttons are arranged horizontally or vertically
     * @param dialogResultListener  Listener for beaming back the selected dialog option to the caller
     */
    public void showDialog(String msg,  DialogButton[] buttons,
                           boolean useVerticalButtonList, DialogResultListener dialogResultListener) {
        showDialog(new Label(msg, skin), null, buttons, useVerticalButtonList, dialogResultListener);
    }

    public void showDialog(Table contentTable, Array<Actor> dialogFocusableActorArray,
                           DialogButton[] buttons, boolean useVerticalButtonList,
                           DialogResultListener dialogResultListener) {
        showDialog((Actor) contentTable, dialogFocusableActorArray, buttons,
                useVerticalButtonList, dialogResultListener);
    }

    public void showOKDialog(String msg, boolean useVerticalButtonList, DialogResultListener dialogResultListener) {
        showDialog(new Label(msg, skin), null, new DialogButton[]{DialogButton.OK},
                useVerticalButtonList, dialogResultListener);
    }

    private void showDialog(Actor content, Array<Actor> dialogFocusableActorArray, DialogButton[] buttons,
                            boolean useVerticalButtonList, final DialogResultListener dialogResultListener) {
        final Array<Actor> backupCurrentActorArray = new Array<>(this.focusableActorArray);
        final Actor backupFocusedActor = this.currentFocusedActor;

        final Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                Dialog dialog;
                try {
                    dialog = dialogs.pop();
                } catch (IllegalStateException e) {
                    // Edge case; buttons were pressed frantically
                    return;
                }

                focusableActorArray = backupCurrentActorArray;
                currentFocusedActor = backupFocusedActor;
                if (currentFocusedActor != null) {
                    focus(currentFocusedActor);
                }

                if (dialogResultListener != null) {
                    dialogResultListener.dialogResult((DialogButton) object);
                }

                dialog.hide(Actions.parallel(
                        Actions.scaleTo(.6f, .6f, .3f, Interpolation.swingIn),
                        Actions.fadeOut(.2f, Interpolation.smooth)
                ));
            }
        };

        dialog.key(Input.Keys.ESCAPE, null);
        dialog.key(Input.Keys.BACK, null);
        dialog.button(Input.Buttons.BACK, null);

        dialogs.add(dialog);

        dialog.getContentTable().add(content).pad(20f * scaleFactor).padBottom(0f * scaleFactor);
        dialog.getButtonTable().defaults().width(200f * scaleFactor);
        dialog.getButtonTable().pad(10f * scaleFactor);
        dialog.setScale(0.6f);
        dialog.getColor().a = 0;

        if (content instanceof Label) {
            Label label = (Label) content;
            label.setAlignment(Align.center);
        }

        this.focusableActorArray.clear();
        if (dialogFocusableActorArray != null) {
            this.focusableActorArray.addAll(dialogFocusableActorArray);
            this.focusableActorArray.add(null);
        }

        for (DialogButton button : buttons) {
            TextButton textButton = new TextButton(button.toString(), skin);
            dialog.button(textButton, button);
            this.focusableActorArray.add(textButton);
            if (useVerticalButtonList) {
                dialog.getButtonTable().row();
                this.focusableActorArray.add(null);
            }
        }

        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            // Unlikely that people are going to use keyboards with playing on Android
            // Looks a bit weird with the focus in those cases. So just defocus
            defocus(backupFocusedActor);
        } else {
            // Focus the first button in the Dialog when on Desktop
            focus(this.focusableActorArray.get(0));
        }

        if (dialogs.size == 1) {
            // Darken background only for the first Dialog
            Window.WindowStyle darkBackgroundStyle = new Window.WindowStyle(dialog.getStyle());
            darkBackgroundStyle.stageBackground = new TextureRegionDrawable(dialogBackgroundTexture);
            dialog.setStyle(darkBackgroundStyle);
        }

        dialog.show(this, Actions.parallel(
                Actions.scaleTo(1f, 1f, .3f, Interpolation.swingOut),
                Actions.fadeIn(.2f, Interpolation.smooth)
        ));

        dialog.setOrigin(Align.center);
        dialog.setPosition(Math.round((getWidth() - dialog.getWidth()) / 2), Math.round((getHeight() - dialog.getHeight()) / 2));
    }

    public void resize(int width, int height) {
        getViewport().update(width, height, true);
        for (Dialog dialog : dialogs) {
            dialog.setPosition(Math.round((getWidth() - dialog.getWidth()) / 2), Math.round((getHeight() - dialog.getHeight()) / 2));
        }
    }

    /**
     * Fetches the Actor in front of the currently focused Actor to focus
     *
     * @return The next Actor
     */
    private Actor getNextActor() {
        if (currentFocusedActor == null) {
            return getFirstActor();
        }

        int index = focusableActorArray.indexOf(currentFocusedActor, true);
        index++;
        while (index < focusableActorArray.size) {
            Actor nextActor = focusableActorArray.get(index);
            if (nextActor != null && !nextActor.equals(currentFocusedActor)) {
                return nextActor;
            }
            index++;
        }

        return getFirstActor();
    }

    /**
     * Fetches the Actor behind the currently focused Actor to focus
     *
     * @return The Actor behind
     */
    private Actor getPreviousActor() {
        if (currentFocusedActor == null) {
            return getLastActor();
        }

        int index = focusableActorArray.indexOf(currentFocusedActor, true);
        index--;
        while (index >= 0) {
            Actor nextActor = focusableActorArray.get(index);
            if (nextActor != null && !nextActor.equals(currentFocusedActor)) {
                return nextActor;
            }
            index--;
        }

        return getLastActor();
    }

    /**
     * Fetches the Actor below the currently focused Actor to focus
     *
     * @return The Actor below
     */
    private Actor getBelowActor() {
        if (currentFocusedActor == null) {
            return getFirstActor();
        }

        int colIndex = 0;
        boolean searchRow = false;
        int actorIndex = -1;
        for (int i = 0; i < focusableActorArray.size; i++) {
            Actor actor = focusableActorArray.get(i);
            if (actor == null) {
                colIndex = 0;
                if (actorIndex != -1) {
                    searchRow = true;
                }
                continue;
            } else if (searchRow && colIndex == actorIndex) {
                return actor;
            } else if (actor.equals(currentFocusedActor)) {
                actorIndex = colIndex;
            }

            colIndex++;
        }

        return getFirstActor();
    }

    /**
     * Fetches the Actor above the currently focused Actor to focus
     *
     * @return The Actor above
     */
    private Actor getAboveActor() {
        if (currentFocusedActor == null) {
            return getLastActor();
        }

        int colIndex = 0;
        boolean searchRow = false;
        int actorIndex = -1;
        for (int i = focusableActorArray.size - 1; i >= 0; i--) {
            Actor actor = focusableActorArray.get(i);
            if (actor == null) {
                colIndex = 0;
                if (actorIndex != -1) {
                    searchRow = true;
                }
                continue;
            } else if (searchRow && colIndex == actorIndex) {
                return actor;
            } else if (actor.equals(currentFocusedActor)) {
                actorIndex = colIndex;
            }

            colIndex++;
        }

        return getLastActor();
    }

    /**
     * Fetches the first non-null actor from the Array
     *
     * @return The first Actor
     */
    private Actor getFirstActor() {
        for (Actor actor : focusableActorArray) {
            if (actor != null) {
                return actor;
            }
        }

        return null;
    }

    /**
     * Fetches the last non-null actor from the Array
     *
     * @return The last Actor
     */
    private Actor getLastActor() {
        for (int i = focusableActorArray.size - 1; i >= 0; i--) {
            Actor actor = focusableActorArray.get(i);
            if (actor != null) {
                return actor;
            }
        }

        return null;
    }

    /**
     * Determines if an Actor is "busy" or not
     * Busy means that the particular input key could have a certain behavior on the Widget
     * Example: Pressing the down arrow key when the focus is on an open SelectBox should not
     * change its focus as it has the behavior of highlighting its next option for the down key
     *
     * <p>
     * Currently, implemented custom behavior for
     * - TextField
     * - SelectBox
     *
     * @param keyCode The input key's keycode
     * @return true if the Actor is not "busy"
     * false if the Actor is "busy"
     */
    private boolean actorIsNotBusy(int keyCode) {
        if (currentFocusedActor instanceof TextField) {
            if (keyCode == Input.Keys.SPACE) {
                return false;
            }

            TextField textField = (TextField) currentFocusedActor;
            if (keyCode == Input.Keys.LEFT) {
                return textField.getCursorPosition() == 0 && textField.getSelection().isEmpty();
            } else if (keyCode == Input.Keys.RIGHT) {
                return textField.getCursorPosition() == textField.getText().length() && textField.getSelection().isEmpty();
            }
        } else if (currentFocusedActor instanceof DropDownMenu<?>) {
            DropDownMenu<?> selectBox = (DropDownMenu<?>) currentFocusedActor;
            boolean isExpanded = selectBox.isExpanded();

            if (isExpanded) {
                if (keyCode == Input.Keys.DOWN || keyCode == Input.Keys.UP ||
                        keyCode == Input.Keys.ENTER || keyCode == Input.Keys.SPACE) {
                    return false;
                }

                // Collapse the SelectBox as some other key was pressed
                selectBox.hideList();
            }
        }

        return true;
    }

    /**
     * Called to send a touchDown event to the currently focused Actor
     *
     * @return true if there is an Actor that is focused
     * false if not
     */
    private boolean triggerTouchDown() {
        if (currentFocusedActor != null) {
            downActor = currentFocusedActor;

            InputEvent event = new InputEvent();
            event.setPointer(-1);
            event.setType(InputEvent.Type.touchDown);
            currentFocusedActor.fire(event);

            return true;
        }
        return false;
    }

    /**
     * Clears the set up focusable array
     */
    public void clearFocusableArray() {
        defocus(currentFocusedActor);
        focusableActorArray.clear();
    }

    /**
     * Called to send a touchUp event to the currently focused Actor
     *
     * @return true if there is an Actor that is focused
     * false if not
     */
    private boolean triggerTouchUp() {
        if (downActor != null) {
            InputEvent event = new InputEvent();
            event.setPointer(-1);
            event.setType(InputEvent.Type.touchUp);
            downActor.fire(event);
            downActor = null;

            return true;
        }
        return false;
    }

    /**
     * Used to set the master switch for focus management
     *
     * @param active Boolean to set the focus management active or not
     */
    public void setFocusManagementActive(boolean active) {
        this.isActive = active;

        // Reset some parameters
        currentFocusedActor = downActor = null;
    }

    /**
     * Handles focus management by checking the keyboard input keycodes and calling the right methods
     *
     * @param keyCode The input key's keycode
     * @return true if the key was handled
     * false if not
     */
    @Override
    public boolean keyDown(int keyCode) {
        if (!isActive || downActor != null || !actorIsNotBusy(keyCode)) {
            return super.keyDown(keyCode);
        }

        boolean handled = false, shiftHeldDown = (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));

        if (keyCode == Input.Keys.ENTER || keyCode == Input.Keys.SPACE) {
            handled = triggerTouchDown();
        } else if ((keyCode == Input.Keys.TAB && !shiftHeldDown) || keyCode == Input.Keys.RIGHT) {
            handled = setFocusedActor(getNextActor());
        } else if ((keyCode == Input.Keys.TAB && shiftHeldDown) || keyCode == Input.Keys.LEFT) {
            handled = setFocusedActor(getPreviousActor());
        } else if (keyCode == Input.Keys.DOWN) {
            handled = setFocusedActor(getBelowActor());
        } else if (keyCode == Input.Keys.UP) {
            handled = setFocusedActor(getAboveActor());
        }

        if (handled) {
            return true;
        } else {
            return super.keyDown(keyCode);
        }
    }

    /**
     * Handles focus management by checking the keyboard input keycodes and calls the right methods
     *
     * @param keyCode The input key's keycode
     * @return true if the key was handled
     * false if not
     */
    @Override
    public boolean keyUp(int keyCode) {
        if (!isActive) {
            return super.keyUp(keyCode);
        }

        if (keyCode == Input.Keys.ENTER || keyCode == Input.Keys.SPACE) {
            boolean handled = triggerTouchUp();
            if (handled) {
                return true;
            }
        }

        return super.keyUp(keyCode);
    }

    /**
     * Handles proper focus management in case another Actor was focused without using the keyboard
     *
     * <p>
     * Params same as the super class Stage. Refer it's documentation for more insight about them
     */
    @Override
    public void addTouchFocus(EventListener listener, Actor listenerActor, Actor target, int pointer, int button) {
        if (!isActive) {
            super.addTouchFocus(listener, listenerActor, target, pointer, button);
            return;
        }

        // Couldn't access Stage's mouse hover actor as it is private
        if (focusableActorArray.contains(target, true)) {
            setFocusedActor(target);
        } else {
            // An Actor outside the provided focusable array was focused, say, by using the mouse
            // In that case, defocus the current actor to show focus loss, but keep the actor reference
            // so that we can start from it back again once the keyboard is used once again
            Actor backup = currentFocusedActor;
            defocus(currentFocusedActor);
            currentFocusedActor = backup;
        }

        super.addTouchFocus(listener, listenerActor, target, pointer, button);
    }

    public boolean dialogIsActive() {
        return dialogs.size > 0;
    }

    @Override
    public void dispose() {
        dialogBackgroundTexture.dispose();
        super.dispose();
    }

    public interface DialogResultListener {
        void dialogResult(DialogButton button);
    }

    public enum DialogButton {
        OK("OK"),
        Cancel("Cancel"),
        Set("Set"),
        Yes("Yes"),
        Kick("Kick"),
        Resume("Resume"),
        MainMenu("Main Menu"),
        Exit("Exit"),
        Restart("Restart"),
        SetMap("Set Map");

        private final String buttonText;

        DialogButton(String buttonText) {
            this.buttonText = buttonText;
        }

        @Override
        public String toString() {
            return this.buttonText;
        }
    }
}
