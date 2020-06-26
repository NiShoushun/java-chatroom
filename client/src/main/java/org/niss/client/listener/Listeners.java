package org.niss.client.listener;

import javafx.scene.control.TextArea;

import java.util.List;

/**
 * @author Ni187
 */
public class Listeners {
    private List<Listener> listeners;

    public void setTextArea(TextArea area){
        for(Listener listener : listeners){
            listener.setTextArea(area);
        }
    }

    public Listeners(List<Listener> listeners){
        this.listeners = listeners;
    }

    public void add(Listener listener){
        this.listeners.add(listener);
    }

    public List<Listener> getListeners(){
        return this.listeners;
    }

    public void setListeners(List<Listener> listeners){
        this.listeners = listeners;
    }

}
