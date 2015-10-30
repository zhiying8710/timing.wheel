package me.binge.timing.wheel.tick;

import java.util.List;
import java.util.concurrent.Semaphore;

import me.binge.timing.wheel.utils.ZookeeperConstant;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

public class ZookeeprTickCondition implements TickCondition {

    private ZkClient zkClient;
    private String workPath;
    private static final Semaphore tickSemaphore = new Semaphore(1);

    public ZookeeprTickCondition(String workPath, ZkClient zkClient) {
        this.zkClient = zkClient;
        this.workPath = workPath;
    }

    protected void tryOccupy(String path) {
        try {
            zkClient.createEphemeral(path + "/" + ZookeeperConstant.TICK_OCCUPY_NODE_NAME);
        } catch (ZkNodeExistsException zknee) {
            return;
        } catch (Exception e) {
            return;
        }
        tickSemaphore.release();
    }

    @Override
    public boolean tick() {
        try {
            tickSemaphore.acquire();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final String path = ZookeeperConstant.ROOT + "/" + workPath;
        zkClient.createPersistent(path, true);
        zkClient.subscribeChildChanges(path, new IZkChildListener() {

            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds)
                    throws Exception {
                if (currentChilds != null && !currentChilds.isEmpty()) {
                    return;
                }
                tryOccupy(path);
            }
        });
        tryOccupy(path);

        try {
            tickSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

}
