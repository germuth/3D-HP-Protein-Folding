/*
 * ConnMan.java
 *
 * Created on March 24, 2009, 4:40 PM
 */

package gui;

/**
 *
 * @author  hartsho
 */
public class ConnMan extends javax.swing.JFrame {
    
    /** Creates new form ConnMan */
    public ConnMan(Gui ui) {
        initComponents();
        this.ui = ui;
        if(ui.getServerName() != null){
            Communicate test_connection = new Communicate(ui.getServerName(), null);
            connection_status.setText(test_connection.testAddress());
            this.server_name.setText(ui.getServerName());
        }
    }
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        connection_status = new javax.swing.JTextField();
        server_name = new javax.swing.JTextField("localhost");
        test_connection = new javax.swing.JButton();
        set_server_name = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Connection Status");
        setAlwaysOnTop(true);
        setResizable(false);

        connection_status.setEditable(false);
        connection_status.setFont(new java.awt.Font("DejaVu Sans", 1, 13));
        connection_status.setText("Server not specified");
        connection_status.setBorder(null);
        connection_status.setOpaque(false);

        test_connection.setText("Test Connection");
        test_connection.setEnabled(false);
        test_connection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                test_connectionActionPerformed(evt);
            }
        });

        set_server_name.setText("O.k");
        set_server_name.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                set_server_nameActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(server_name, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                    .addComponent(connection_status, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(test_connection)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 144, Short.MAX_VALUE)
                        .addComponent(set_server_name, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(connection_status, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(server_name, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(test_connection)
                    .addComponent(set_server_name))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void test_connectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_test_connectionActionPerformed
       Communicate test_connection = new Communicate(server_name.getText(), null);
       connection_status.setText(test_connection.testAddress());
    }//GEN-LAST:event_test_connectionActionPerformed

    private void set_server_nameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_set_server_nameActionPerformed
        setServerName();
        dispose();
    }//GEN-LAST:event_set_server_nameActionPerformed
    
private void setServerName(){
    ui.setServerName(server_name.getText());
}
    Gui ui;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField connection_status;
    private javax.swing.JTextField server_name;
    private javax.swing.JButton set_server_name;
    private javax.swing.JButton test_connection;
    // End of variables declaration//GEN-END:variables
    
}
