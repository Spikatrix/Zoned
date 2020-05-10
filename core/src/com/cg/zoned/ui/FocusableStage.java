package com.cg.zoned.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The FocusableStage is basically a Stage with focus managing capabilities on Actors
 * This means that it can track and focus actors when the user navigates using the keyboard
 *
 * <p>
 * Custom behavior implemented for:
 * - TextField
 * - SelectBox
 *
 * <p>
 * Currently supported keyboard inputs include:
 * - TAB            for focusing the next Actor
 * - SHIFT + TAB    for focusing the previous Actor
 * - LEFT           for focusing the previous Actor
 * - RIGHT          for focusing the next Actor
 * - UP             for focusing the above Actor
 * - DOWN           for focusing the below Actor
 * - Enter or Space for pressing the currently focused Actor
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
    private Array<Actor> focusableActorArray = new Array<Actor>();

    /**
     * Holds the currently focused actor
     */
    private Actor currentFocusedActor;

    /**
     * Holds the actor that is held down
     */
    private Actor downActor;

    /**
     * Dialog that has focus properties for its buttons
     */
    private Dialog dialog;

    /**
     * Master switch for enabling keyboard based focus management
     * When this is false, it will behave like a regular Stage
     */
    private boolean isActive = true;

    /**
     * Constructor for initializing the Stage
     *
     * @param viewport Viewport for the super class Stage
     */
    public FocusableStage(Viewport viewport) {
        super(viewport);
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
                ((TextField) actor).setTextFieldListener(new TextField.TextFieldListener() {
                    @Override
                    public void keyTyped(TextField textField, char c) {
                        /*
                         * Gets rid of those pesky tabs that get inserted into the TextField
                         * when tab navigating through them
                         */
                        if (c == TAB) {
                            textField.setText(textField.getText().replaceAll("\t", ""));
                        }
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
        this.setKeyboardFocus(actor);
        this.setScrollFocus(actor);
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
            this.setKeyboardFocus(null);
            this.setScrollFocus(null);
        }
    }

    /**
     * Shows an dialog with focus properties on its buttons
     *
     * @param msg The message to display
     * @param buttonTexts Texts for each button in the dialog
     * @param useVerticalButtonList Determines whether the dialog buttons are arranged horizontally or vertically
     * @param scaleFactor The game's scaleFactor to scale up/down dialog button width
     * @param dialogResultListener Interface for beaming back the selected dialog option
     * @param skin The skin to use for the dialog
     */
    public void showDialog(String msg, Array<String> buttonTexts,
                           boolean useVerticalButtonList,
                           float scaleFactor, DialogResultListener dialogResultListener, Skin skin) {
        showDialog(new Label(msg, skin), buttonTexts, useVerticalButtonList, scaleFactor, dialogResultListener, skin);
    }

    public void showDialog(Table contentTable, Array<String> buttonTexts,
                           boolean useVerticalButtonList,
                           float scaleFactor, DialogResultListener dialogResultListener, Skin skin) {
        showDialog((Actor) contentTable, buttonTexts, useVerticalButtonList, scaleFactor, dialogResultListener, skin);
    }

    private void showDialog(Actor content, Array<String> buttonTexts,
                            boolean useVerticalButtonList,
                            float scaleFactor, final DialogResultListener dialogResultListener, Skin skin) {
        final Array<Actor> backupCurrentActorArray = new Array<Actor>(this.focusableActorArray);
        final Actor backupFocusedActor = this.currentFocusedActor;

        final Dialog previousDialog = dialog;

        dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                focusableActorArray = backupCurrentActorArray;
                if (backupFocusedActor != null) {
                    focus(backupFocusedActor);
                }

                if (dialogResultListener != null) {
                    dialogResultListener.dialogResult((String) object);
                }

                dialog.hide(Actions.scaleTo(0, 0, .2f, Interpolation.fastSlow));
                dialog = previousDialog;
            }
        };

        dialog.getContentTable().add(content).pad(20f);
        dialog.getButtonTable().defaults().width(200f * scaleFactor);
        dialog.getButtonTable().padBottom(10f).padLeft(10f).padRight(10f);
        dialog.setScale(0);
        // TODO: Focus props on content table

        if (content instanceof Label) {
            Label label = (Label) content;
            label.setAlignment(Align.center);
        }

        this.focusableActorArray.clear();
        for (int i = 0; i < buttonTexts.size; i++) {
            TextButton textButton = new TextButton(buttonTexts.get(i), skin);
            dialog.button(textButton, textButton.getText().toString());
            this.focusableActorArray.add(textButton);
            if (useVerticalButtonList) {
                dialog.getButtonTable().row();
                this.focusableActorArray.add(null);
            }
        }

        dialog.show(this, Actions.scaleTo(1f, 1f, .2f, Interpolation.fastSlow));
        dialog.setOrigin(dialog.getWidth() / 2, dialog.getHeight() / 2);
        dialog.setPosition(Math.round((getWidth() - dialog.getWidth()) / 2), Math.round((getHeight() - dialog.getHeight()) / 2));
        focus(this.focusableActorArray.get(0));
    }

    public void resize(int width, int height) {
        getViewport().update(width, height, true);
        if (dialog != null) {
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
        } else if (currentFocusedActor instanceof DropDownMenu) {
            DropDownMenu selectBox = (DropDownMenu) currentFocusedActor;
            boolean isExpanded = selectBox.isExpanded();

            if (isExpanded) {
                if (keyCode == Input.Keys.DOWN || keyCode == Input.Keys.UP ||
                        keyCode == Input.Keys.ENTER || keyCode == Input.Keys.SPACE) {
                    return false;
                }

                // Collapse the SelectBox as some other key was pressed
                triggerTouchDown();
                triggerTouchUp();
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

        if (focusableActorArray.contains(target, true)) {
            setFocusedActor(target);
        } else {
            defocus(currentFocusedActor);
        }

        super.addTouchFocus(listener, listenerActor, target, pointer, button);
    }

    public interface DialogResultListener {
        void dialogResult(String buttonText);
    }
}
