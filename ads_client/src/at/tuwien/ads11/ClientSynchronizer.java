package at.tuwien.ads11;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.tuwien.ads11.remote.ClientMock;
import at.tuwien.ads11.remote.Movement;

public class ClientSynchronizer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ClientSynchronizer.class);

    private boolean run;

    private int wait;

    private int prevIndex;

    private AlcatrazClient client;

    public ClientSynchronizer(AlcatrazClient client, int time, int numId) {
        this.setWait(time);
        this.setRun(true);
        this.client = client;
        this.initPrevIndex(numId);
    }

    @Override
    public void run() {
        while (isRun()) {
            try {

                Thread.sleep(this.wait);
                this.synchronize();

            } catch (RemoteException e) {
                LOG.warn("An error occurred while trying to synchronize: {}", e.getMessage());
                this.rebindStub();
                
            } catch (InterruptedException e) {
            }
        }
    }

    private void rebindStub() {
        ClientMock mock = this.client.getClients().get(this.prevIndex);
        try {
            IClient stub = this.client.getStub(mock);
            if (stub != null) {
                this.client.getClientStubCache().remove(this.prevIndex);
                this.client.getClientStubCache().add(this.prevIndex, stub);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public synchronized void synchronize() throws RemoteException {
        List<Movement> remote = client.getClientStubCache().get(this.getPrevIndex()).getHistory();
        List<Movement> local = client.getLocalHistory();

        List<Movement> delta = this.getDelta(local, remote);
        this.applyDelta(delta);

    }

    private void applyDelta(List<Movement> delta) {
        for (Movement m : delta) {
            this.client.applyMove(m); // local call
        }

    }

    private List<Movement> getDelta(List<Movement> local, List<Movement> remote) {
        List<Movement> delta = new ArrayList<Movement>();

        if (local.size() < remote.size()) {
            LOG.info("client is not up to date, synchronizing...");
            int diff = remote.size() - local.size();
            diff--; // fix index
            while (diff >= 0) {
                Movement m = remote.get(diff);
                delta.add(0, m);
                diff--;
            }
        }

        return delta;
    }

    private void initPrevIndex(int numId) {
        switch (numId) {
        case 0:
            this.setPrevIndex(this.client.getClientStubCache().size() - 1);
            break;
        case 1:
            this.setPrevIndex(0);
            break;
        case 2:
            this.setPrevIndex(1);
            break;
        case 3:
            this.setPrevIndex(2);
            break;
        }
    }

    public void setRun(boolean run) {
        this.run = run;
    }

    public boolean isRun() {
        return run;
    }

    public void setWait(int wait) {
        this.wait = wait;
    }

    public int getWait() {
        return wait;
    }

    public void setPrevIndex(int prevIndex) {
        this.prevIndex = prevIndex;
    }

    public int getPrevIndex() {
        return prevIndex;
    }
}
