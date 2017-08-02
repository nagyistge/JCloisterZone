package com.jcloisterzone.ui.grid;

import static com.jcloisterzone.ui.I18nUtils._;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import com.jcloisterzone.figure.neutral.Mage;
import com.jcloisterzone.figure.neutral.Witch;
import com.jcloisterzone.game.state.GameState;
import com.jcloisterzone.ui.GameController;
import com.jcloisterzone.ui.component.MultiLineLabel;
import com.jcloisterzone.ui.gtk.ThemedJLabel;
import com.jcloisterzone.wsio.message.MoveNeutralFigureMessage;

public class SelectMageWitchRemovalPanel extends JPanel {


    public SelectMageWitchRemovalPanel(final GameController gc) {
        setOpaque(true);
        setBackground(gc.getClient().getTheme().getTransparentPanelBg());
        setLayout(new MigLayout("ins 10 20 10 20", "[grow]", ""));

        JLabel label;

        label = new ThemedJLabel(_("Mage and Witch"));
        label.setFont(CornCirclesPanel.FONT_HEADER);
        label.setForeground(gc.getClient().getTheme().getHeaderFontColor());
        add(label, "wrap, gapbottom 10");

        MultiLineLabel mll = new MultiLineLabel(_("It''s not possible to place mage or witch because there isn''t an unfinished feature. Select what figure do you want to remove from board."));
        add(mll, "wrap, growx, gapbottom 5");

        GameState state = gc.getGame().getState();
        boolean isActive = state.getActivePlayer().isLocalHuman();

        JButton btn = new JButton();
        btn.setText(_("Remove the mage."));
        btn.setEnabled(isActive);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((JButton)e.getSource()).setEnabled(false);
                Mage mage = state.getNeutralFigures().getMage();
                gc.getConnection().send(
                    new MoveNeutralFigureMessage(gc.getGameId(), mage.getId(), null));
            }
        });
        add(btn, "wrap, growx, h 40, gapbottom 5");

        btn = new JButton();
        btn.setText(_("Remove the witch."));
        btn.setEnabled(isActive);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((JButton)e.getSource()).setEnabled(false);
                Witch witch = state.getNeutralFigures().getWitch();
                gc.getConnection().send(
                    new MoveNeutralFigureMessage(gc.getGameId(), witch.getId(), null));
            }
        });
        add(btn, "wrap, growx, h 40, gapbottom 5");
    }

}
