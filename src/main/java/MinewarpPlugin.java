package net.bot2k3.siebe.Minewarp;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.*;

/**
 * Provides the main plugin interface.
 */
public class MinewarpPlugin extends JavaPlugin
{
    private HashMap<String, YamlConfiguration> players;

    /**
     * Occurs when the plugin is being enabled.
     */
    public void onEnable()
    {
        this.players = new HashMap<String, YamlConfiguration>();
    }

    /**
     * Occurs when the plugin is being disabled.
     */
    public void onDisable()
    {
    }

    /**
     * Occurs when a command has been sent.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // get the player from the server.
        Player player = this.getServer().getPlayerExact(sender.getName());
        if (player != null)
        {
            // get the configuration for the sender.
            YamlConfiguration config = this.getPlayerConfig(player);

            // then check the name of command to see what we need to do.
            String name = command.getName();
            if (name.equals("warp"))
            {
                // handle the warp command.
                this.onCommandWarp(player, config, args);
                return true;
            }
            else if (name.equals("home"))
            {
                // handle the "/home" command.
                this.onCommandHome(player, config, args);
                return true;
            }
        }

        return false;
    }

    /**
     * Handles the /warp command.
     */
    private void onCommandWarp(Player player, YamlConfiguration config, String[] args)
    {
        if (args.length > 0)
        {
            if (args[0].equals("set"))
            {
                if (args.length >= 2)
                {
                    // let's set a warp point!
                    this.onCommandWarpSet(player, config, args);
                    return;
                }
            }
            else if (args[0].equals("del") ||
                     args[0].equals("delete"))
            {
                // let's delete the warp point :-(
                this.onCommandWarpDelete(player, config, args);
                return;
            }
            else if (args[0].equals("list"))
            {
                // list all the warp points.
                this.onCommandWarpList(player, config, args);
                return;
            }
            else
            {
                // check to see if the player has the location in their list.
                this.onCommandWarpUse(player, config, args);
                return;
            }
        }

        // if we're still here we need to send usage.
        player.sendMessage(
            new String[] {
				ChatColor.AQUA + "/warp set <name> [public|private] " + ChatColor.WHITE + "- Adds the current location to your warp list",
				ChatColor.AQUA + "/warp del <name> [public|private] " + ChatColor.WHITE + "- Removes a warp point",
				ChatColor.AQUA + "/warp list " + ChatColor.WHITE + "- Lists all your warp points",
				ChatColor.AQUA + "/warp <name> " + ChatColor.WHITE + "- Transports you to a warp point"
			});
    }

    /**
     * Handles the /warp set command.
     */
    private void onCommandWarpSet(Player player, YamlConfiguration config, String[] args)
    {
        // get the name of the warp, and check it's validity.
        String name = args[1];

        if (Pattern.matches("^[A-Za-z0-9]+$", name))
        {
            // check whether the user wants to make this public or private.
            String visibility = "private";

            if (args.length >= 3)
            {
                args[2] = args[2].toLowerCase();

                if (args[2].equals("public") ||
                    args[2].equals("private"))
                {
                    visibility = args[2];
                }
                else
                {
                    // we can only use "public" or "private".
                    player.sendMessage(ChatColor.RED + "The visibility \"" + args[2] + "\" is invalid -- You can only use \"public\" or \"private\".");
                    return;
                }
            }

            // okay then! add this to the list.
            if (visibility.equals("public"))
            {
                this.setLocation(null, this.getPlayerConfig(null), name, player.getLocation());
            }
            else
            {
                this.setLocation(player, config, name, player.getLocation());
            }

            // send a quick confirmation.
            player.sendMessage(ChatColor.GREEN + "The warp point was added.");
        }
        else
        {
            // the name can only contain alphanumeric characters.
            player.sendMessage(ChatColor.RED + "The name \"" + name + "\" is invalid -- You can only a-z0-9.");
        }
    }

    /**
     * Handles the /warp delete command.
     */
    private void onCommandWarpDelete(Player player, YamlConfiguration config, String[] args)
    {
        String name = args[1];
        String[] visibility = new String[] { "private", "public" };

        if (args.length >= 3)
        {
            // we're explicitly being told to check for a visibility.
            args[2] = args[2].toLowerCase();

            if (args[2].equals("public") ||
                args[2].equals("private"))
            {
                visibility = new String[] { args[2] };
            }
            else
            {
                // no such visibility, boo!
                player.sendMessage(ChatColor.RED + "The visibility \"" + visibility + "\" is invalid. You can only use \"public\" or \"private\"");
                return;
            }
        }

        boolean removed = false;

        for (int i = 0, len = visibility.length; !removed && (i < len); i++)
        {
            if (visibility[i].equals("public"))
            {
                // attempt to remove from the public repository.
                removed = this.removeLocation(null, this.getPlayerConfig(null), name);
            }
            else
            {
                // attempt to remove from the private repository.
                removed = this.removeLocation(player, config, name);
            }
        }

        if (removed)
        {
            // success! the warp point was removed.
            player.sendMessage(ChatColor.GREEN + "The warp point \"" + name + "\" was removed.");
        }
        else
        {
            // bummer, the warp point doesn't exist.
            player.sendMessage(ChatColor.RED + "The warp point \"" + name + "\" does not exist.");
        }
    }

    /**
     * Handles the /warp list command.
     */
    private void onCommandWarpList(Player player, YamlConfiguration config, String[] args)
    {
        // list all the warp points, including public ones.
        player.sendMessage(
            new String[] {
				ChatColor.WHITE + "Public: " + this.getWarpList(this.getPlayerConfig(null)),
				ChatColor.WHITE + "Private: " + this.getWarpList(config)
			});
    }

    /**
     * Handles the /warp command.
     */
    private void onCommandWarpUse(Player player, YamlConfiguration config, String[] args)
    {
        Location location = this.getLocation(config, args[0]);

        if (location == null)
        {
            // attempt to get the location from the public list then.
            location = this.getLocation(this.getPlayerConfig(null), args[0]);
        }

        if (location != null)
        {
            player.teleport(location);
        }
        else
        {
            // this warp point does not exist :-(
            player.sendMessage(ChatColor.RED + "The warp point \"" + args[0] + "\" does not exist.");
        }
    }

    /**
     * Handles the /home command.
     */
    private void onCommandHome(Player player, YamlConfiguration config, String[] args)
    {
        if (args.length == 0)
        {
            // take the player home.
            Location location = this.getLocation(config, "home");
            if (location != null)
            {
                player.teleport(location);
            }
            else
            {
                // the location does not exist, so notify the user.
                player.sendMessage(ChatColor.RED + "You must set your home location first using \"" + ChatColor.AQUA + "/home set" + ChatColor.RED + "\".");
            }
        }
        else if (args[0].equals("set"))
        {
            // set the new home location.
            this.setLocation(player, config, "home", player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Your home location has been set.");
        }
        else
        {
            // send usage about about the "home" command.
            player.sendMessage(
                new String[]
				{
					ChatColor.AQUA + "/home " + ChatColor.WHITE + "- Transports you home",
					ChatColor.AQUA + "/home set " + ChatColor.WHITE + "- Sets your home location"
				});
        }
    }

    /**
     * Gets a list of warp points.
     */
    private String getWarpList(YamlConfiguration config)
    {
        Map values = config.getValues(false);
        Set keys = values.keySet();

        // set up a string writer ("StringBuilder")
        StringWriter writer = new StringWriter();

        Iterator iterator = keys.iterator();
        int i = 0;

        while (iterator.hasNext())
        {
            if (i > 0)
            {
                writer.write(ChatColor.WHITE.toString());
                writer.write(", ");
            }

            if (i % 2 == 0)
            {
                writer.write(ChatColor.YELLOW.toString());
            }
            else
            {
                writer.write(ChatColor.AQUA.toString());
            }

            writer.write(" ");
            writer.write((String)iterator.next());

            i++;
        }

        return writer.toString();
    }

    /**
     * Gets a player name.
     */
    private String getPlayerName(Player player)
    {
        if (player != null)
        {
            return player.getName();
        }

        return "__public__";
    }

    /**
     * Gets the config path of a player.
     */
    private File getPlayerConfigPath(Player player)
		throws IOException
    {
        return new File(this.getDataFolder(), this.getPlayerName(player) + ".yml");
    }

    /**
     * Gets a players configuration.
     */
    private YamlConfiguration getPlayerConfig(Player player)
    {
        String name = this.getPlayerName(player);

        if (this.players.containsKey(name))
        {
            // we have a cached instance of this config file.
            return this.players.get(name);
        }

        YamlConfiguration config = new YamlConfiguration();
        config.options().pathSeparator('/');

        try
        {
            File file = this.getPlayerConfigPath(player);
            if (file.exists())
            {
                // the existing configuration file exists, so simply open it up.			
                config.load(file);
            }
            else
            {
                // save the empty configuration to the file.
                config.save(file);
            }
        }
        catch (Exception e)
        {
        }

        return config;
    }

    /**
     * Gets a location for a given config instance.
     */
    private Location getLocation(YamlConfiguration config, String name)
    {
        ConfigurationSection section = config.getConfigurationSection(name);

        if (section != null)
        {
            if (section.contains("world") &&
                section.contains("x") &&
                section.contains("y") &&
                section.contains("z"))
            {
                // found the section, now get the world, x, y and z.
                String n = section.getString("world");

                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");

                double pitch = section.getDouble("pitch");
                double yaw = section.getDouble("yaw");

                // attempt to get the world from the server -- it has to exist, of course.
                World world = this.getServer().getWorld(n);
                if (world != null)
                {
                    return new Location(world, x, y, z, (float)yaw, (float)pitch);
                }
            }
        }

        return null;
    }

    /**
     * Saves a new location.
     */
    private void setLocation(Player player, YamlConfiguration config, String name, Location location)
    {
        if (!config.contains(name) || !config.isConfigurationSection(name))
        {
            // create the new section first.
            config.set(name, null);
            config.createSection(name);
        }

        // we can use the existing configuration section.
        ConfigurationSection section = config.getConfigurationSection(name);

        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("pitch", location.getPitch());
        section.set("yaw", location.getYaw());

        try
        {
            // save the configuration.
            config.save(this.getPlayerConfigPath(player));
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Removes a location.
     */
    private boolean removeLocation(Player player, YamlConfiguration config, String name)
    {
        if (config.contains(name) && config.isConfigurationSection(name))
        {
            // just remove the location.
            config.set(name, null);

            try
            {
                // once removed, commit the change to disk.
                config.save(this.getPlayerConfigPath(player));
            }
            catch (Exception e)
            {
            }

            return true;
        }

        return false;
    }
}
