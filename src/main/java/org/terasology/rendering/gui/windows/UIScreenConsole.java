/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.gui.windows;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertex2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.vecmath.Vector2f;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;
import org.terasology.entitySystem.Prefab;
import org.terasology.input.events.KeyEvent;
import org.terasology.input.binds.ConsoleButton;
import org.terasology.input.binds.PauseButton;
import org.terasology.game.CoreRegistry;
import org.terasology.input.BindButtonEvent;
import org.terasology.logic.console.ConsoleResult;
import org.terasology.logic.console.GroovyHelp;
import org.terasology.logic.console.GroovyHelpManager;
import org.terasology.logic.console.GroovyManager;
import org.terasology.rendering.gui.widgets.UIText;
import org.terasology.rendering.gui.widgets.UITextWrap;
import org.terasology.rendering.gui.widgets.UIWindow;
import org.terasology.utilities.StringConstants;

/**
 * The debug console of Terasology.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 * 
 * TODO rewrite this. add a own class for the console which will handle commands, maybe handle "chat" in general inside the class and console commands will be special chat commands.
 */
public final class UIScreenConsole extends UIWindow {

    private Logger logger = Logger.getLogger(getClass().getName());
    private final UIText _consoleText;
    private final UITextWrap _helpText;

    private final StringBuffer _consoleInput = new StringBuffer();
    private final ArrayList<String> _ringBuffer = new ArrayList<String>();
    private final int border = 70;
    private int _ringBufferPos = -1;

    /**
     * Init. a new Terasology console.
     */
    public UIScreenConsole() {
        setModal(true);
        setCloseBinds(new String[] {ConsoleButton.ID});
        setCloseKeys(new int[] {Keyboard.KEY_ESCAPE});
        setCloseBinds(new String[] {PauseButton.ID});
        
        setPosition(new Vector2f(0, 0));
        setSize(new Vector2f(Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight()));

        //setScrollBarsPosition(new Vector2f(Display.getWidth() / 2,Display.getHeight() / 2),new Vector2f(0.5f,0.5f));

        _consoleText = new UIText();
        _consoleText.setVisible(true);
        addDisplayElement(_consoleText);

        _helpText = new UITextWrap();
        _helpText.setColor(Color.green);
        //_helpText.setSize(new Vector2f(0.5f,0.5f));
        _helpText.setVisible(true);

        addDisplayElement(_helpText);
    }

    /**
     * Processes the given keyboard input.
     *
     * @param event The key event
     */
    public void processKeyboardInput(KeyEvent event) {
        super.processKeyboardInput(event);

        if (!isVisible())
            return;

        if (!event.isDown()) {
            return;
        }

        switch (event.getKey()) {
            case Keyboard.KEY_BACK:
                int length = _consoleInput.length() - 1;

                if (length < 0) {
                    length = 0;
                }
                _consoleInput.setLength(length);
                event.consume();
                break;
            case Keyboard.KEY_RETURN:
                processConsoleString();
                event.consume();
                break;
            case Keyboard.KEY_UP:
                rotateRingBuffer(1);
                event.consume();
                break;
            case Keyboard.KEY_DOWN:
                rotateRingBuffer(-1);
                event.consume();
                break;
            default:
                char c = event.getKeyCharacter();

                if (!Character.isISOControl(c)) {
                    _consoleInput.append(c);
                    event.consume();
                }
                break;
        }
    }

    @Override
    public void processBindButton(BindButtonEvent event) {
        if (!isVisible() || !event.isDown()) {
            return;
        }

        if (ConsoleButton.ID.equals(event.getId())) {
            close();
            event.consume();
        }
    }

    /**
     * Rotates the ring buffer in the given direction.
     *
     * @param dir The direction
     */
    public void rotateRingBuffer(int dir) {
        if (_ringBuffer.isEmpty())
            return;

        _ringBufferPos += dir;

        if (_ringBufferPos < 0)
            _ringBufferPos = _ringBuffer.size() - 1;
        else if (_ringBufferPos > _ringBuffer.size() - 1)
            _ringBufferPos = 0;

        _consoleInput.setLength(0);
        _consoleInput.append(_ringBuffer.get(_ringBufferPos));
    }

    /**
     * Adds a new string to the ring buffer.
     */
    public void addToRingBuffer() {
        _ringBufferPos = -1;
        _ringBuffer.add(0, _consoleInput.toString());
    }

    /**
     * Parses the console string and executes the command.
     */
    public void processConsoleString() {
        boolean success = false;
        _helpText.resetScroll();
        try {
            if (_consoleInput.toString().startsWith("help")) {
                getHelp(_consoleInput.toString());
                addToRingBuffer();
                resetDebugConsole();
                return;
            } else {
                ConsoleResult result = CoreRegistry.get(GroovyManager.class).runGroovyShell(_consoleInput.toString());
                if (result.isSuccess()) {
                    setHelpText(result.getDisplayString());
                    logger.log(Level.INFO, "Console command \"{0}\" accepted.", _consoleInput);

                    addToRingBuffer();
                    resetDebugConsole();
                } else {
                    try {
                        _helpText.loadError();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    logger.log(Level.WARNING, "Console command \"{0}\" is invalid.", _consoleInput);
                }
            }
        } catch (Exception e) {
            logger.log(Level.INFO, e.getMessage());
        }
    }

    private void getHelp(String commandstring) throws IOException {
        String[] split = commandstring.split(" ");
        GroovyHelpManager groovyhelpmanager = new GroovyHelpManager();
        if (split.length > 1) {
            if (split[1].equals("commandList")) {
                HashMap<String, String> commandhelp = groovyhelpmanager.getHelpCommands();
                String[] commandlist = groovyhelpmanager.getGroovyCommands();
                String retval = "Available commands :" + StringConstants.NEW_LINE + StringConstants.NEW_LINE;
                for (int i = 0; i < commandlist.length; i++) {
                    if (commandhelp.containsKey(commandlist[i])) {
                        retval += commandlist[i].toString() + " \t: " + commandhelp.get(commandlist[i]).toString() + StringConstants.NEW_LINE;
                    } else {
                        retval += commandlist[i].toString() + " : undocumented" + StringConstants.NEW_LINE;
                    }
                }
                setHelpText(retval);
                return;
            }

            if (groovyhelpmanager.getHelpCommands().containsKey(split[1])) {
                GroovyHelp groovyhelp = groovyhelpmanager.readCommandHelp(split[1]);
                setHelpText(groovyhelp);
                return;
            }
            if (split[1].equals("itemList")) {
                String tempval = "";
                ArrayList<Prefab> prefabs = groovyhelpmanager.getItems();
                Iterator<Prefab> it = prefabs.iterator();
                while (it.hasNext()) {
                    tempval += "item = " + it.next().getName() + StringConstants.NEW_LINE;
                }
                setHelpText(tempval);
                return;
            }
            showError();
        } else {
            showHelp();
        }
    }

    public String toString() {
        return _consoleInput.toString();
    }

    /**
     * Resets the debug console.
     */
    public void resetDebugConsole() {
        _consoleInput.setLength(0);
    }

    public void setHelpText(String helptekst) throws IOException {
        _helpText.addText(helptekst);
        _helpText.showFromJson();
    }

    public void setHelpText(GroovyHelp groovyhelp) throws IOException {
        String tempstring = "";
        tempstring += groovyhelp.getCommandName() + " : " + groovyhelp.getCommandDesc() + StringConstants.NEW_LINE;
        if (groovyhelp.getParameters().length > 0) {
            String parameters = "accepted parameters :" + StringConstants.NEW_LINE;
            for (int i = 0; i < groovyhelp.getParameters().length; i++) {
                parameters += groovyhelp.getParameters()[i] + StringConstants.NEW_LINE;
            }
            tempstring += parameters + StringConstants.NEW_LINE;
        }
        tempstring += "detailed help : " + groovyhelp.getCommandHelp() + StringConstants.NEW_LINE;
        if (groovyhelp.getExamples().length > 0) {
            String examples = "Example(s) :" + StringConstants.NEW_LINE;
            for (int i = 0; i < groovyhelp.getExamples().length; i++) {
                examples += groovyhelp.getExamples()[i] + StringConstants.NEW_LINE;
            }
            tempstring += examples + StringConstants.NEW_LINE;
            _helpText.addText(tempstring);
            _helpText.showFromJson();
        }
    }

    public void showHelp() throws IOException {
        _helpText.loadHelp();
    }

    public void showError() throws IOException {
        _helpText.loadError();
    }

    @Override
    public void update() {
        super.update();

        _consoleText.setText(String.format("%s_", this));
        _consoleText.setPosition(new Vector2f(4, Display.getHeight() - 16 - 4));
        _helpText.setPosition(new Vector2f(4, 12));
    }

    public void render() {
        renderOverlay();
        super.render();
    }

    public void renderOverlay() {
        glPushMatrix();
        glLoadIdentity();
        glColor4f(0, 0, 0, 0.75f);
        glBegin(GL_QUADS);
        glVertex2f(0f, 0f);
        glVertex2f((float) Display.getWidth() - border, 0f);
        glVertex2f((float) Display.getWidth() - border, (float) Display.getHeight() - border);
        glVertex2f(0f, (float) Display.getHeight() - border);
        glEnd();
        glBegin(GL_QUADS);
        glVertex2f(0f, Display.getHeight() - 24);
        glVertex2f((float) 300, Display.getHeight() - 24);
        glVertex2f((float) 300, (float) Display.getHeight());
        glVertex2f(0f, (float) Display.getHeight());
        glEnd();
        glPopMatrix();
    }
}
