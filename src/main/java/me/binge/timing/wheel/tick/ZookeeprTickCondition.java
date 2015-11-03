package me.binge.timing.wheel.tick;

import java.util.List;

import me.binge.timing.wheel.utils.ZookeeperConstant;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

public class ZookeeprTickCondition implements TickCondition {

    private ZkClient zkClient;
    private String workPath;
    private volatile boolean occupy = false;

    public ZookeeprTickCondition(String workPath, ZkClient zkClient) {
        this.zkClient = zkClient;
        this.workPath = ZookeeperConstant.ROOT + "/" + workPath;

        zkClient.createPersistent(this.workPath, true);
        zkClient.subscribeChildChanges(this.workPath, new IZkChildListener() {

            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds)
                    throws Exception {
                if (currentChilds != null && !currentChilds.isEmpty()) {
                    return;
                }
                tryOccupy();
            }
        });
    }

    protected boolean tryOccupy() {
        if (occupy) {
            return true;
        }
        try {
            zkClient.createEphemeral(workPath + "/" + ZookeeperConstant.TICK_OCCUPY_NODE_NAME);
        } catch (ZkNodeExistsException zknee) {
            return false;
        } catch (Exception e) {
            return false;
        }
        occupy = true;
        return true;
    }

    @Override
    public boolean tick() {
        return tryOccupy();
    }

    @Override
    public void untick() {
        this.occupy = false;
    }

}
