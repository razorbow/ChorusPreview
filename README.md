# Chorus Preview — Fabric 1.21.11

Created by **razorbow**.

A client-side mod that displays possible chorus fruit landing regions and an
estimated success chance. The overlay supports fill, outline, and combined
highlight modes. The HUD can show either full diagnostic details or only the
chance. Russian and English interfaces are included.

Every marker is a 0.62-block square centered on the top face, preventing large
or offset visual regions.

Install `chorus-preview-1.2.2-mc1.21.11.jar` in `mods`. Minecraft 1.21.11,
Java 21, Fabric Loader 0.18.4+, and Fabric API are required. Mod Menu 17.0.0 is
optional and provides convenient access to settings.

The overlay appears only while regular chorus fruit is held in either hand.
Press H to toggle the mod.

This is a client-side prediction based on the world state known to the client.
It does not reveal the server RNG, send packets, or alter teleport mechanics.
