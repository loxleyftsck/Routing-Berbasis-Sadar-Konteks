/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.testContext;

import core.DTNHost;
import core.SimClock;
import routing.EnergyAwareRouter;
import routing.MessageRouter;

/**
 *
 * @author LENOVO
 */
public class EnergyManager {

    private static double lastUpdateTime = SimClock.getTime();

    /**
     * Mengurangi energi node berdasarkan aktivitasnya (scanning dan
     * transmitting).
     *
     * @param host Node yang sedang diperiksa
     */
    public static void updateEnergy(DTNHost host) {
        MessageRouter router = host.getRouter();

        if (router instanceof EnergyAwareRouter energyRouter) {
            ////
            double currentEnergy = energyRouter.getCurrentEnergy();
            System.out.println("Before update, Node " + host.getAddress() + " Energy: " + currentEnergy);
            /////

            // Hitung selisih waktu sejak update terakhir
            double currentTime = SimClock.getTime();
            double deltaTime = currentTime - lastUpdateTime;

            // Update lastUpdateTime untuk iterasi berikutnya
            lastUpdateTime = currentTime;

            // Ambil nilai scanning dan transmitting cost dari router
            double scanningCost = energyRouter.getScanEnergy();
            double transmittingCost = energyRouter.getTransmitEnergy() * deltaTime;

            // Kurangi energi dengan metode consumeEnergy
            energyRouter.consumeEnergy(scanningCost + transmittingCost);

            ////
             System.out.println("After update, Node " + host.getAddress() + " Energy: " + energyRouter.getCurrentEnergy());
             /////
             
             
            // Cek apakah energi habis
            if (energyRouter.getCurrentEnergy() <= 0) {
                System.out.println("Node " + host.getAddress() + " kehabisan energi!");
            } else {
                System.out.println("Node " + host.getAddress() + " Energi tersisa: " + energyRouter.getCurrentEnergy());
            }
        }
    }

    /**
     * Mendapatkan level energi yang tersisa dari EnergyAwareRouter.
     *
     * @param router Router dari node
     * @return Energi yang tersisa
     */
    public static double getRemainingEnergy(EnergyAwareRouter router) {
     
         return router.getCurrentEnergy();
    }

    public static void consumeEnergy(EnergyAwareRouter router, double amount) {
        router.consumeEnergy(amount);
    }
}

//
// /**
//     * Mengurangi energi node berdasarkan aktivitasnya (scanning dan transmitting).
//     *
//     * @param host Node yang sedang diperiksa
//     */
//    public static void updateEnergy(DTNHost host) {
//        MessageRouter router = host.getRouter();
//
//        if (router instanceof EnergyAwareRouter) {
//            EnergyAwareRouter energyRouter = (EnergyAwareRouter) router;
//
//            // Ambil energi saat ini
//            double currentEnergy = energyRouter.getCurrentEnergy();
//
//            // Hitung selisih waktu sejak update terakhir
//            double currentTime = SimClock.getTime();
//            double deltaTime = currentTime - energyRouter.getLastUpdateTime();
//
//            // Update waktu terakhir
//            energyRouter.setLastUpdateTime(currentTime);
//
//            // Asumsi konsumsi energi
//            double scanningCost = 0.5;  // Energi untuk scanning
//            double transmittingCost = 2.0;  // Energi per detik saat mengirim data
//
//            // Kurangi energi berdasarkan aktivitas
//            energyRouter.reduceEnergy(scanningCost);
//            energyRouter.reduceEnergy(transmittingCost * deltaTime);
//
//            // Cek apakah energi habis
//            if (energyRouter.getCurrentEnergy() <= 0) {
//                System.out.println("Node " + host.getAddress() + " kehabisan energi!");
//            } else {
//                System.out.println("Node " + host.getAddress() + " Energi tersisa: " + energyRouter.getCurrentEnergy());
//            }
//        }
//    }
//    private static double lastUpdateTime = SimClock.getTime();
//
//    /**
//     * Mengurangi energi node berdasarkan aktivitasnya (scanning dan
//     * transmitting).
//     *
//     * @param host Node yang sedang diperiksa
//     */
//    public static void updateEnergy(DTNHost host) {
//        MessageRouter router = host.getRouter();
//
//        if (router instanceof EnergyAwareRouter) {
//            EnergyAwareRouter energyRouter = (EnergyAwareRouter) router;
//
//            // Ambil energi saat ini
//            double currentEnergy = getRemainingEnergy(energyRouter);
//
//            // Hitung selisih waktu sejak update terakhir
//            double currentTime = SimClock.getTime();
//            double deltaTime = currentTime - lastUpdateTime;
//
//            // Update lastUpdateTime untuk iterasi berikutnya
//            lastUpdateTime = currentTime;
//
//            // Asumsi konsumsi energi (bisa diubah sesuai kebutuhan)
//            double scanningCost = 0.5;  // Energi untuk scanning
//            double transmittingCost = 2.0;  // Energi per detik saat mengirim data
//
//            // Kurangi energi berdasarkan aktivitas
//            currentEnergy -= scanningCost;
//            currentEnergy -= transmittingCost * deltaTime;
//
//            // Cek apakah energi habis
//            if (currentEnergy <= 0) {
//                System.out.println("Node " + host.getAddress() + " kehabisan energi!");
//            } else {
//                System.out.println("Node " + host.getAddress() + " Energi tersisa: " + currentEnergy);
//            }
//        }
//    }
//    /**
//     * Mendapatkan level energi yang tersisa dari router
//     *
//     * @param router Router dari node
//     * @return Energi yang tersisa, atau -1 jika gagal membaca
//     */
//    public static double getRemainingEnergy(EnergyAwareRouter router) {
//        try {
//            java.lang.reflect.Field energyField = EnergyAwareRouter.class.getDeclaredField("energy");
//            energyField.setAccessible(true);
//            return energyField.getDouble(router);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            return -1.0; // Jika gagal membaca energi
//        }
//    }
//
//    /**
//     * Mengurangi energi node berdasarkan jumlah energi yang dikonsumsi.
//     *
//     * @param router Router dari node yang menggunakan energi
//     * @param amount Jumlah energi yang dikonsumsi
//     */
//    public static void consumeEnergy(EnergyAwareRouter router, double amount) {
//        try {
//            java.lang.reflect.Field energyField = EnergyAwareRouter.class.getDeclaredField("energy");
//            energyField.setAccessible(true);
//            double currentEnergy = energyField.getDouble(router);
//
//            // Kurangi energi
//            double newEnergy = currentEnergy - amount;
//            if (newEnergy < 0) {
//                newEnergy = 0; // Hindari negatif
//            }
//            // Simpan nilai baru
//            energyField.setDouble(router, newEnergy);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            System.err.println("Gagal mengurangi energi: " + e.getMessage());
//        }
//    }

