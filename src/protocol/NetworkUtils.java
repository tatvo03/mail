package protocol;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtils {

    /**
     * Lấy danh sách tất cả các địa chỉ IPv4 LAN khả dụng của máy tính
     */
    public static List<String> getAllLocalIpAddresses() {
        List<String> ipList = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (!ipList.contains(ip)) {
                            ipList.add(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi quét IP mạng: " + e.getMessage());
        }

        if (ipList.isEmpty()) {
            ipList.add("127.0.0.1");
        }
        return ipList;
    }

    /**
     * Lấy địa chỉ IP chính để hiển thị cho các máy khác kết nối tới
     */
    public static String getPrimaryIpAddress() {
        List<String> list = getAllLocalIpAddresses();
        // Ưu tiên dải mạng LAN thông dụng (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
        for (String ip : list) {
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                return ip;
            }
        }
        return list.get(0);
    }
}

