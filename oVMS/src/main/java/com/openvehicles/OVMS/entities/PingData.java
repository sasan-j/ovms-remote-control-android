package com.openvehicles.OVMS.entities;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

/**
 * Created by jay on 15/04/15.
 */
public class PingData {
    private static int PINGS_DISCARD = 2;
    private long sendTime = 0;
    private long arrivalTime = 0;
    private long duration = -1;

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
        this.duration = this.arrivalTime - this.sendTime;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    public long getDuration(){
        return this.duration;
    }

    public static String nanoToMilliString(long nanoseconds){
        long tt = TimeUnit.MILLISECONDS.convert(nanoseconds, TimeUnit.NANOSECONDS);
        return String.valueOf(tt);
    }

    public static long computeAverageDuration(ArrayList<PingData> pingDataArrayList){
        long sum = 0;
        for(int i=PINGS_DISCARD; i<pingDataArrayList.size();i++){
            sum += pingDataArrayList.get(i).getDuration();
        }

        return sum/(pingDataArrayList.size()-PINGS_DISCARD);
    }
}
