package org.niss.client.listener;


import javafx.scene.control.TextArea;

import java.util.List;

/**
 * @author Ni187
 */
public interface Listener extends Runnable{
    void setTextArea(TextArea textArea);
    void setName(String name);
    void setUserList(List<String> list);
}
