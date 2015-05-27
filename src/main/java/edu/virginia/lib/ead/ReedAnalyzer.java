package edu.virginia.lib.ead;

/**
 * Created by md5wz on 3/10/15.
 */
public class ReedAnalyzer {

    public static void main(String [] args) throws Exception {
        new ReedAnalyzer().getEADPidList();
        
    }
    
    private void getEADPidList() throws Exception {
        XMLEADProcessor p = new XMLEADProcessor();
        p.addVisitor(new EADNodeVisitor() {
            @Override
            public void init() throws Exception {

            }

            @Override
            public void visit(EADNode component) throws Exception {
                if (component.getLevel().equalsIgnoreCase("Collection")) {
                    //System.out.println("RootId: " + component.getReferenceId());
                }
                if (component.getPid() == null) {
                    System.out.println(component.getLevel() + "(" + component.getReferenceId() + ") has no pid: " + component.getTitle());

                } else {
                   //pidsInEAD.add(component.getPid());
                   //eadPidToTypeMap.put(component.getPid(), component.getLevel());
                }
            }

            @Override
            public void finish() throws Exception {

            }
        });
        p.processEADXML(getClass().getClassLoader().getResourceAsStream("viuh00010.xml"));

    }
}
